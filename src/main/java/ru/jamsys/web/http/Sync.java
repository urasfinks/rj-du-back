package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.PromiseExtension;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;

/*
 * Синхронизация данных на фронте
 * */
@Component
@RequestMapping
public class Sync implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public Sync(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1000L)
                .extension(PromiseExtension::thenSelectUuidDevice)
                .then("init", (_, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMap(ServletHandler.class);
                    String data = servletHandler.getRequestReader().getData();
                })
                .extension(PromiseExtension::addTerminal);
    }

}
