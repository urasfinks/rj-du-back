package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.PromiseExtension;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.HttpAsyncResponse;
import ru.jamsys.core.flat.util.JsonSchema;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;

import java.util.Map;


@Component
@RequestMapping
public class GetCode implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public GetCode(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1000L)
                .then("init", (atomicBoolean, promise) -> {
                    //schema/http/GetCode.json
                    //{"mail":"urasfinks@yandex.ru"}
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    String data = input.getHttpRequestReader().getData();
                    JsonSchema.validate(data, UtilFileResource.getAsString("schema/http/GetCode.json"));
                    Map<String, Object> map = UtilJson.getMapOrThrow(data);
                    promise.setMapRepository("mail", map.get("mail"));
                })
                .extension(PromiseExtension::addHandler);
    }

}
