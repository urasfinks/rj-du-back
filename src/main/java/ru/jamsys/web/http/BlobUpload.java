package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
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
import ru.jamsys.promise.repository.AuthRepository;
import ru.jamsys.promise.repository.ParsedJsonRepository;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Map;

/*
 * Получение статических больших данных (binary large object) из БД
 * */
@Component
@RequestMapping
public class BlobUpload implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public BlobUpload(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1000L)
                .extension(PromiseExtension::thenSelectIdUserRequire)
                .extension(PromiseExtension::addParsedJsonRepository)
                .extension(PromiseExtension::addAuthRepository)
                .then("init", (_, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    Map<String, String> map = servletHandler.getRequestReader().getMap();
                    System.out.println(map);
                    if (!map.containsKey("uuid")) {
                        throw new RuntimeException("Uuid is empty");
                    }
                    if (!map.containsKey("file")) {
                        throw new RuntimeException("file is empty");
                    }
                    AuthRepository authRepository = promise.getRepositoryMapClass(AuthRepository.class);
                    if (authRepository.getIdUser() == null || authRepository.getIdUser() != 1) {
                        throw new RuntimeException("Permission denied");
                    }
                    InputStream file = servletHandler.getRequestReader().getMultiPartFormData("file");
                    if (file == null) {
                        throw new RuntimeException("InputStream file is null");
                    }
                    promise.getRepositoryMapClass(ParsedJsonRepository.class)
                            .append("uuid_data", map.get("uuid"))
                            .append("value_data", UtilBase64.base64Encode(file.readAllBytes(), true))
                            .append("type_data", "blob")
                            .append("parent_uuid_data", null)
                            .append("date_add_data", new BigDecimal(System.currentTimeMillis() / 1000))
                            .append("is_remove_data", 0)
                            .append("id_user", authRepository.getIdUser())
                            .append("key_data", map.get("key"))
                            .append("uuid_device", authRepository.getUuidDevice())
                            .append("new_id_revision", new BigDecimal(0));
                })
                .thenWithResource("db", JdbcResource.class, "default", (_, promise, jdbcResource) -> {
                    jdbcResource.execute(new JdbcRequest(Data.INSERT)
                            .addArg(promise.getRepositoryMapClass(ParsedJsonRepository.class))
                    );
                })
                .extension(PromiseExtension::addTerminal);
    }

}
