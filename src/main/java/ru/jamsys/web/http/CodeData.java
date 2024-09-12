package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.ManagerCodeLink;
import ru.jamsys.ManagerCodeLinkItem;
import ru.jamsys.promise.PromiseExtension;
import ru.jamsys.promise.repository.ResponseRepository;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
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
 * Получение данных в свободном доступе по uuid_data в течении срока обслуживания ManagerCodeLink
 * Прикол в том, что данные должны быть в ManagerCodeLink - это значит, что их реально создали для общего пользования
 * Там данные создаются через генерацию кода для подключения
 * */
@Component
@RequestMapping
public class CodeData implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public CodeData(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1000L)
                .extension(PromiseExtension::addResponseRepository)
                .then("init", (_, promise) -> {
                    //{"uuid":"uudData"}
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    String data = servletHandler.getRequestReader().getData();
                    JsonSchema.validate(data, UtilFileResource.getAsString("schema/http/Data.json"), "Data.json");
                    Map<String, Object> parsedJson = UtilJson.getMapOrThrow(data);

                    String uuidData = (String) parsedJson.get("uuid");
                    if (uuidData.trim().isEmpty()) {
                        throw new RuntimeException("uuid is empty");
                    }
                    ManagerCodeLinkItem byUuidData = App.get(ManagerCodeLink.class).getByUuidData(uuidData);
                    if (byUuidData == null) { //Если данные не зарегистрированы в ManagerCodeLink - значит они не публичные
                        throw new RuntimeException("Data not found in " + ManagerCodeLink.class.getName());
                    }
                    promise.setRepositoryMap("uuidData", uuidData);
                })
                .thenWithResource("db", JdbcResource.class, "default", (_, promise, jdbcResource) -> {
                    String uuidData = promise.getRepositoryMap(String.class, "uuidData");
                    List<Map<String, Object>> execute = jdbcResource.execute(
                            new JdbcRequest(Data.SELECT).addArg("uuid_data", uuidData)
                    );
                    if (execute.isEmpty()) {
                        throw new RuntimeException("Data not found in DB");
                    }
                    promise.getRepositoryMapClass(ResponseRepository.class)
                            .append("data", execute.getFirst().get("value_data"));
                })
                .extension(PromiseExtension::addTerminal);
    }

}
