package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.ManagerCodeLink;
import ru.jamsys.ManagerCodeLinkItem;
import ru.jamsys.promise.PromiseExtension;
import ru.jamsys.promise.repository.AuthRepository;
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
 * Генерация кода для авторизации. Если нет пользователя -> insert. Отправляем код на указанную почту.
 * */
@Component
@RequestMapping
public class CodeGenerate implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public CodeGenerate(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1000L)
                .extension(PromiseExtension::addResponseRepository)
                .extension(PromiseExtension::thenSelectIdUserRequire)
                .then("init", (_, _, promise) -> {
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
                    ManagerCodeLinkItem find = App.get(ManagerCodeLink.class).getByUuidData(uuidData);
                    if (find != null) {
                        promise.setRepositoryMap("code", find.getCode());
                        promise.goTo("finish");
                    }
                })
                .thenWithResource("db", JdbcResource.class, "default", (_, _, promise, jdbcResource) -> {
                    String uuidData = promise.getRepositoryMap(String.class, "uuidData");
                    AuthRepository authRepository = promise.getRepositoryMapClass(AuthRepository.class);
                    List<Map<String, Object>> execute = jdbcResource.execute(
                            new JdbcRequest(Data.CHECK_PERMISSION_SOCKET_DATA)
                                    .addArg(authRepository.get())
                                    .addArg("uuid_data", uuidData)
                    );
                    if (execute.isEmpty()) {
                        throw new RuntimeException("Permission denied");
                    }
                    ManagerCodeLinkItem add = App.get(ManagerCodeLink.class).add(uuidData);
                    promise.setRepositoryMap("code", add.getCode());
                })
                .then("finish", (_, _, promise) -> promise.getRepositoryMapClass(ResponseRepository.class)
                        .append("code", promise.getRepositoryMap(Integer.class, "code"))
                        .append("uuid", promise.getRepositoryMap(String.class, "uuidData")))
                .extension(PromiseExtension::addTerminal);
    }

}
