package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.promise.PromiseExtension;

import java.io.File;

/*
 * Получение статических больших данных (binary large object) из БД
 * */
@Component
@RequestMapping("/")
public class Root implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    private final String location;

    public Root(ServicePromise servicePromise, ServiceProperty serviceProperty) {
        this.servicePromise = servicePromise;
        this.location = serviceProperty.get("run.args.web.resource.location");
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1000L)
                .then("init", (_, _, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    servletHandler.writeFileToOutput(new File(location + "index.html"));
                })
                .onComplete((_, _, _) -> {
                })
                .extension(PromiseExtension::addErrorHandler);
    }

}
