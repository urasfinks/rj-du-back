package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.PromiseExtension;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.HttpAsyncResponse;
import ru.jamsys.core.flat.template.twix.TemplateTwix;
import ru.jamsys.core.flat.util.JsonSchema;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.resource.notification.email.EmailNotificationRequest;
import ru.jamsys.core.resource.notification.email.EmailNotificationResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.User;

import java.util.List;
import java.util.Map;

/*
 * Генерация кода для авторизации. Если нет пользователя -> insert. Отправляем код на указанную почту.
 * */
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
                .then("init", (_, promise) -> {
                    //{"mail":"urasfinks@yandex.ru"}
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    String data = input.getHttpRequestReader().getData();
                    JsonSchema.validate(data, UtilFileResource.getAsString("schema/http/GetCode.json"));
                    Map<String, Object> map = UtilJson.getMapOrThrow(data);
                    promise.setMapRepository("mail", map.get("mail"));
                    promise.setMapRepository("code", Util.random(100000, 999999));
                })
                .thenWithResource("db", JdbcResource.class, "default", (_, promise, jdbcResource) -> {
                    Map<String, Object> arg = new HashMapBuilder<String, Object>()
                            .append("mail", promise.getRepositoryMap("mail", String.class))
                            .append("code", promise.getRepositoryMap("code", Integer.class));

                    List<Map<String, Object>> user = jdbcResource.execute(new JdbcRequest(User.GET_BY_MAIL).addArg(arg));
                    if (user.isEmpty()) {
                        jdbcResource.execute(new JdbcRequest(User.INSERT).addArg(arg));
                    } else {
                        jdbcResource.execute(new JdbcRequest(User.SET_CODE).addArg(arg));
                    }
                }).thenWithResource("email", EmailNotificationResource.class, (_, promise, emailNotificationResource) -> {
                    Integer code = promise.getRepositoryMap("code", Integer.class);
                    String mail = promise.getRepositoryMap("mail", String.class);
                    emailNotificationResource.execute(new EmailNotificationRequest(
                            "Ваш код: *****",
                            "Ваш код: " + code,
                            TemplateTwix.template(
                                    Util.getWebContent("email.html"),
                                    new HashMapBuilder<String, String>()
                                            .append("code", code + "")
                                            .append("support", emailNotificationResource.getSupport())
                            ),
                            mail
                    ));
                })
                .extension(PromiseExtension::addHandler);
    }

}