package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.promise.PromiseExtension;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.Data;

/*
 * Генерация кода для авторизации. Если нет пользователя -> insert. Отправляем код на указанную почту.
 * */
@Component
@RequestMapping
public class LogoutWithRemove implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public LogoutWithRemove(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1000L)
                .extension(PromiseExtension::thenSelectIdUser)
                .thenWithResource("db", JdbcResource.class, "default", (_, promise, jdbcResource)
                        -> jdbcResource.execute(new JdbcRequest(Data.REMOVE_ALL)
                        .addArg("id_user", promise.getRepositoryMap("id_user", Integer.class))
                        .addArg("uuid_device", promise.getRepositoryMap("uuid_device", String.class))
                ))
                .extension(PromiseExtension::addTerminal);
    }

}
