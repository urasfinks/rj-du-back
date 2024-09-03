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
import ru.jamsys.core.web.http.HttpHandler;

import java.util.Map;

/*
 * Получить uuid по коду
 * */
@Component
@RequestMapping
public class CodeUuid implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public CodeUuid(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1000L)
                .extension(PromiseExtension::addResponseRepository)
                .then("init", (_, promise) -> {
                    //{"code": 267936}
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    String data = servletHandler.getRequestReader().getData();
                    JsonSchema.validate(data, UtilFileResource.getAsString("schema/http/CodeUuid.json"), "CodeUuid.json");
                    Map<String, Object> parsedJson = UtilJson.getMapOrThrow(data);

                    Integer code = (Integer) parsedJson.get("code");
                    ManagerCodeLinkItem find = App.get(ManagerCodeLink.class).getByCode(code);
                    if (find == null) {
                        throw new Exception("Code not found in " + ManagerCodeLink.class.getName());
                    }
                    promise.getRepositoryMapClass(ResponseRepository.class)
                            .append("code", find.getCode())
                            .append("uuid", find.getUuidData());
                })
                .extension(PromiseExtension::addTerminal);
    }

}
