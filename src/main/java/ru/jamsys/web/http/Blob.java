package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.UtilBase64;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.Data;
import ru.jamsys.promise.PromiseExtension;
import ru.jamsys.promise.repository.ParsedJsonRepository;

import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/*
 * Получение статических больших данных (binary large object) из БД
 * */
@Component
@RequestMapping
public class Blob implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public Blob(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1000L)
                .extension(PromiseExtension::addParsedJsonRepository)
                .then("init", (_, _, promise) -> {
                    //?uuid=a1b2c3
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    Map<String, String> map = servletHandler.getRequestReader().getMap();
                    if (!map.containsKey("uuid")) {
                        throw new RuntimeException("Uuid is empty");
                    }
                    ParsedJsonRepository parsedJsonRepository = promise.getRepositoryMapClass(ParsedJsonRepository.class);
                    parsedJsonRepository.put("uuid_data", map.get("uuid"));
                })
                .thenWithResource("db", JdbcResource.class, "default", (_, _, promise, jdbcResource) -> {
                    ParsedJsonRepository parsedJsonRepository = promise.getRepositoryMapClass(ParsedJsonRepository.class);
                    List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(Data.SELECT_SYSTEM_STATIC)
                                    .addArg(parsedJsonRepository)
                    );
                    System.out.println(execute);
                    if (execute.isEmpty()) {
                        throw new RuntimeException("Data not found");
                    }
                    parsedJsonRepository.put("blob", execute.getFirst().get("value_data"));
                })
                .onComplete((_, task, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    try {
                        ParsedJsonRepository parsedJsonRepository = promise.getRepositoryMapClass(ParsedJsonRepository.class);
                        String uuidData = (String) parsedJsonRepository.get("uuid_data");
                        String mimeType = URLConnection.guessContentTypeFromName(uuidData);
                        if (mimeType == null) {
                            throw new RuntimeException("MimeType " + uuidData + " not defined");
                        }
                        String contentType = MediaType.valueOf(mimeType).getType();
                        //System.out.println("Content-Type: " + contentType);
                        servletHandler.setResponseContentType(contentType);
                        UtilBase64.base64Decode(
                                new ByteArrayInputStream(((String) parsedJsonRepository.get("blob")).getBytes()),
                                servletHandler.getResponseOutputStream(),
                                true
                        );
                        servletHandler.getServletResponse().complete(null);
                    } catch (Throwable th) {
                        promise.setError(task, th);
                        PromiseExtension.errorHandler(promise);
                    }
                })
                .extension(PromiseExtension::addErrorHandler);
    }

}
