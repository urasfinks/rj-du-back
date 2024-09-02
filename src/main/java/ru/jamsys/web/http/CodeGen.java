package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.CodeManager;
import ru.jamsys.CodeManagerItem;
import ru.jamsys.PromiseExtension;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
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
 * Генерация кода для авторизации. Если нет пользователя -> insert. Отправляем код на указанную почту.
 * */
@Component
@RequestMapping
public class CodeGen implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public CodeGen(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1000L)
                .extension(PromiseExtension::thenSelectIdUser)
                .then("init", (_, promise) -> {
                    //{"uuid":"uudData"}
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    String data = servletHandler.getRequestReader().getData();
                    JsonSchema.validate(data, UtilFileResource.getAsString("schema/http/Data.json"), "Data.json");
                    Map<String, Object> parsedJson = UtilJson.getMapOrThrow(data);

                    String uuidData = (String) parsedJson.get("uuid");
                    if (uuidData == null || uuidData.trim().isEmpty()) {
                        throw new RuntimeException("uuid is empty");
                    }
                    promise.setRepositoryMap("uuidData", uuidData);
                })
                .thenWithResource("db", JdbcResource.class, "default", (_, promise, jdbcResource) -> {
                    String uuidData = promise.getRepositoryMap("uuidData", String.class);
                    List<Map<String, Object>> execute = jdbcResource.execute(
                            new JdbcRequest(Data.CHECK_PERMISSION_SOCKET_DATA)
                                    .addArg("id_user", promise.getRepositoryMap("id_user", Integer.class))
                                    .addArg("uuid_device", promise.getRepositoryMap("uuid_device", String.class))
                                    .addArg("uuid_data", uuidData)
                    );
                    if (execute.isEmpty()) {
                        throw new RuntimeException("Permission denied");
                    }
                    CodeManagerItem add = App.get(CodeManager.class).add(uuidData);
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    servletHandler.setResponseBodyFromMap(new HashMapBuilder<String, Object>()
                            .append("code", add.getCode())
                            .append("uuid", add.getUuidData())
                    );
                })
                .extension(PromiseExtension::addTerminal);
    }

}
