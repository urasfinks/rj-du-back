package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.JsonSchema;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.resource.notification.email.EmailNotificationResource;
import ru.jamsys.core.resource.notification.email.EmailTemplateNotificationRequest;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.User;
import ru.jamsys.promise.PromiseExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Генерация кода для авторизации. Если нет пользователя -> insert. Отправляем код на указанную почту.
 * */
@Component
@RequestMapping
public class AuthGetCode implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public AuthGetCode(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 7_000L)
                .then("init", (_, _, promise) -> {
                    //{"mail":"urasfinks@yandex.ru"}
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    String data = servletHandler.getRequestReader().getData();
                    JsonSchema.validate(data, UtilFileResource.getAsString("schema/http/AuthGetCode.json"), "AuthGetCode.json");
                    Map<String, Object> map = UtilJson.getMapOrThrow(data);
                    promise.setRepositoryMap("mail", map.get("mail"));
                    promise.setRepositoryMap("code", Util.random(100000, 999999));
                })
                .thenWithResource("db", JdbcResource.class, "default", (_, _, promise, jdbcResource) -> {
                    Map<String, Object> arg = new HashMapBuilder<String, Object>()
                            .append("mail", promise.getRepositoryMap(String.class, "mail"))
                            .append("code", promise.getRepositoryMap(Integer.class, "code"));

                    List<Map<String, Object>> user = jdbcResource.execute(
                            new JdbcRequest(User.GET_BY_MAIL).addArg(arg));
                    if (user.isEmpty()) {
                        jdbcResource.execute(new JdbcRequest(User.INSERT).addArg(arg));
                    } else {
                        jdbcResource.execute(new JdbcRequest(User.SET_CODE).addArg(arg));
                    }
                })
                .thenWithResource("email", EmailNotificationResource.class, (_, _, promise, emailNotificationResource) -> {
                    Integer code = promise.getRepositoryMap(Integer.class, "code");
                    String mail = promise.getRepositoryMap(String.class, "mail");
                    HashMap<String, String> data = new HashMapBuilder<String, String>()
                            .append("code", code + "")
                            .append("support", emailNotificationResource.getProperty().getSupport());
                    emailNotificationResource.execute(new EmailTemplateNotificationRequest(
                            "Ваш код: *****",
                            "Ваш код: " + code,
                            emailNotificationResource.getProperty().getTemplateClassLoader(),
                            emailNotificationResource.getProperty().getTemplatePath(),
                            data,
                            mail
                    ));
                })
                .extension(PromiseExtension::addTerminal);
    }

}
