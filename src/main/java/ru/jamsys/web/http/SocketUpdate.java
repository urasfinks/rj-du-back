package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.promise.PromiseExtension;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.web.socket.WebSocket;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.JsonSchema;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.Data;

import java.util.List;
import java.util.Map;

/*
 * Обновление сокетных данных
 * Сокетные данные это просто json данные в таблице, обновление это замещение данных по корневым ключам
 * Нельзя обновить какие-то данные внутри структуры, только через полное изменение корневого ключа
 * Ньюанс: у каждой персоны свой json, то есть своя уникальная запись с своим uuid_data
 * На свой собственный uuid_data фронт подписывается на прослушку через сокеты
 * Реально данные хранятся у владельца, а все остальные просто через id_parent ссылаются на запись владельца
 * Обновление происходит записи владельца, а всем наследникам меняется номер ревизии
 * */
@Component
@RequestMapping
public class SocketUpdate implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public SocketUpdate(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1000L)
                .extension(PromiseExtension::thenSelectIdUser)
                .then("init", (_, promise) -> {
                    ServletHandler input = promise.getRepositoryMapClass(ServletHandler.class);
                    String data = input.getRequestReader().getData();
                    JsonSchema.validate(data, UtilFileResource.getAsString("schema/http/UpdateSocketData.json"), "UpdateSocketData.json");
                    Map<String, Object> map = UtilJson.getMapOrThrow(data);
                    promise.setRepositoryMap("uuid_data", map.get("uuid_data"));
                    promise.setRepositoryMap("data", map.get("data"));
                })
                .extension(SocketUpdate::dbUpdate)
                .extension(PromiseExtension::addTerminal);
    }

    public static void dbUpdate(Promise promiseSource) {
        promiseSource.thenWithResource("DbUpdate", JdbcResource.class, "default", (_, promise, jdbcResource) -> {
            Map<String, Object> arg = new HashMapBuilder<String, Object>()
                    .append("id_user", promise.getRepositoryMap("id_user", Integer.class))
                    .append("uuid_device", promise.getRepositoryMap("uuid_device", String.class))
                    .append("uuid_data", promise.getRepositoryMap("uuid_data", String.class));

            List<Map<String, Object>> permission = jdbcResource.execute(new JdbcRequest(Data.CHECK_PERMISSION_SOCKET_DATA).addArg(arg));
            if (permission.isEmpty()) {
                throw new RuntimeException("Permission denied");
            }

            // С этого момента мы начинаем работать без коммитов, поэтому в try, что бы в последствии завершить коммит
            try {
                //Получаем главную запись с данными для последующего обновления
                List<Map<String, Object>> primarySocketData = jdbcResource.execute(new JdbcRequest(Data.GET_PRIMARY_SOCKET_DATA).addArg(arg));
                if (primarySocketData.isEmpty()) {
                    throw new RuntimeException("Primary socket data not found");
                }
                String dbValue = (String) primarySocketData.getFirst().get("value_data");
                //Проверяем, что данные в БД не пустые
                if (dbValue == null || dbValue.trim().isEmpty()) {
                    throw new RuntimeException("Socket data empty");
                }
                Map<String, Object> dbValueMap = UtilJson.getMapOrThrow(dbValue);
                dbValueMap.putAll(promise.getRepositoryMap("data", Map.class));

                //Если любой ключ равен null - удаляем ключ
                removeNullValue(dbValueMap);

                //Обновляем главные сокетные данные
                jdbcResource.execute(new JdbcRequest(Data.UPDATE_PRIMARY_SOCKET_DATA)
                        .addArg("value_data", UtilJson.toStringPretty(dbValueMap, "{}"))
                        .addArg("id_data", primarySocketData.getFirst().get("id_data"))
                );

                //Обновляем ревизии дочерних сокетных данных
                jdbcResource.execute(new JdbcRequest(Data.UPDATE_SECONDARY_SOCKET_DATA)
                        .addArg("uuid_data", promise.getRepositoryMap("uuid_data", String.class))
                );
                //Закинем на обработку данные для рассылки по сокетам
                App.get(WebSocket.class)
                        .notify(
                                promise.getRepositoryMap("uuid_data", String.class),
                                UtilJson.toStringPretty(
                                        new HashMapBuilder<String, Object>()
                                                .append("handler", "SYNC")
                                                .append("uuid_data", promise.getRepositoryMap("uuid_data", String.class)),
                                        "{}"
                                )
                        );
            } catch (Throwable th) {
                jdbcResource.execute(new JdbcRequest(Data.UNLOCK));
                throw new ForwardException(th);
            }
            jdbcResource.execute(new JdbcRequest(Data.UNLOCK));
        });
    }

    public static void removeNullValue(Map<?, ?> references) {
        Object[] objects = references.keySet().toArray();
        for (Object key : objects) {
            if (references.get(key) == null) {
                references.remove(key);
            }
        }
    }

}
