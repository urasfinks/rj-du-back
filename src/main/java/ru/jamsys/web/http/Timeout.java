package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.PromiseExtension;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;

import java.util.Map;

/*
 * Генерация для проверки timeout для фронта
 * */
@Component
@RequestMapping
public class Timeout implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public Timeout(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 30000L)
                .then("init", (_, promise) -> {
                    ServletHandler input = promise.getRepositoryMapClass(ServletHandler.class);
                    Map<String, String> map = input.getRequestReader().getMap();
                    int timeoutMs = 5000;
                    if (map.containsKey("timeout") && Util.isInt(map.get("timeout"))) {
                        timeoutMs = Integer.parseInt(map.get("timeout"));
                    }
                    if (timeoutMs > 30000) { // Но не больше 30 сек
                        timeoutMs = 30000;
                    }
                    Util.sleepMs(timeoutMs);
                })
                .extension(PromiseExtension::addTerminal);
    }

}
