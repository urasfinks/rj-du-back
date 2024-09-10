package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.promise.PromiseExtension;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Получение статических больших данных (binary large object) из БД
 * */
@Component
@RequestMapping("/AuthWeb/**")
public class AuthWeb implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    private final Map<String, String> staticFile = new HashMap<>();

    public AuthWeb(ServicePromise servicePromise, ServiceProperty serviceProperty) {
        this.servicePromise = servicePromise;
        String location = serviceProperty.get("run.args.web.resource.location");
        String absPath = new File(location).getAbsolutePath();
        List<String> filesRecursive = UtilFile.getFilesRecursive(location);
        filesRecursive.forEach(s -> staticFile.put(s.substring(absPath.length()), s));
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1000L)
                .extension(PromiseExtension::thenSelectIdUserRequire)
                .then("init", (_, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    String uri = servletHandler.getRequest().getRequestURI().substring(8);
                    if (staticFile.containsKey(uri)) {
                        servletHandler.writeFileToOutput(new File(staticFile.get(uri)));
                    } else {
                        throw new RuntimeException("Resource not found");
                    }
                })
                .onComplete((_, _) -> {
                })
                .extension(PromiseExtension::addErrorHandler);
    }

}
