package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.JsonSchema;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.promise.PromiseExtension;
import ru.jamsys.promise.repository.ParsedJsonRepository;

/*
 * Обработка комментария с App. Тут будет проверка, что устройство авторизовано.
 * */
@Component
@RequestMapping
public class CommentApp implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public CommentApp(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 30000L)
                .extension(PromiseExtension::addParsedJsonRepository)
                .extension(PromiseExtension::thenSelectIdUserRequire)
                .then("init", (_, _, promise) -> {
                    //{
                    //    "name": "Юрий",
                    //    "email": "urasfinks@yandex.ru",
                    //    "subject": "Тема сообщения",
                    //    "message": "Hello"
                    //}
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    String data = servletHandler.getRequestReader().getData();
                    JsonSchema.validate(
                            data,
                            UtilFileResource.getAsString("schema/http/CommentApp.json"),
                            "CommentApp.json"
                    );
                    promise.getRepositoryMapClass(ParsedJsonRepository.class).putAll(UtilJson.getMapOrThrow(data));
                })
                .extension(CommentWeb::thenSendToTelegram)
                .extension(PromiseExtension::addTerminal);
    }


}
