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
import ru.jamsys.core.resource.google.ReCaptchaResource;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.notification.telegram.TelegramNotificationRequest;
import ru.jamsys.core.resource.notification.telegram.TelegramNotificationResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.promise.PromiseExtension;
import ru.jamsys.promise.repository.ParsedJsonRepository;

import java.util.Map;

/*
 * Обработка комментария с Web. Тут будет проверка на ReCaptcha.
 * */
@Component
@RequestMapping
public class CommentWeb implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public CommentWeb(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 30000L)
                .extension(PromiseExtension::addParsedJsonRepository)
                .then("init", (_, promise) -> {
                    //{
                    //    "name": "Юрий",
                    //    "email": "urasfinks@yandex.ru",
                    //    "subject": "Тема сообщения",
                    //    "message": "Hello",
                    //    "g-recaptcha-response": "12345"
                    //}
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    String data = servletHandler.getRequestReader().getData();
                    JsonSchema.validate(
                            data,
                            UtilFileResource.getAsString("schema/http/CommentWeb.json"),
                            "CommentWeb.json"
                    );
                    promise.getRepositoryMapClass(ParsedJsonRepository.class).putAll(UtilJson.getMapOrThrow(data));
                })
                .thenWithResource("recaptcha", ReCaptchaResource.class, (_, promise, reCaptchaResource) -> {
                    ParsedJsonRepository parsedJsonRepository = promise.getRepositoryMapClass(ParsedJsonRepository.class);
                    HttpResponse httpResponse = reCaptchaResource.execute((String) parsedJsonRepository.get("g-recaptcha-response"));
                    if (!httpResponse.isStatus()) {
                        throw new RuntimeException(httpResponse.getDescription());
                    }
                })
                .extension(CommentWeb::thenSendToTelegram)
                .extension(PromiseExtension::addTerminal);
    }

    public static void thenSendToTelegram(Promise promiseResource) {
        promiseResource
                .thenWithResource("http", TelegramNotificationResource.class, (_, promise, telegramNotificationResource) -> {
                    ParsedJsonRepository parsedJsonRepository = promise.getRepositoryMapClass(ParsedJsonRepository.class);
                    parsedJsonRepository.remove("g-recaptcha-response");
                    HttpResponse httpResponse = telegramNotificationResource.execute(
                            new TelegramNotificationRequest(
                                    (String) parsedJsonRepository.remove("subject"),
                                    UtilJson.toStringPretty(parsedJsonRepository, "{}")
                            )
                    );
                    if (!httpResponse.isStatus()) {
                        throw new RuntimeException(httpResponse.getDescription());
                    }
                });
    }

}
