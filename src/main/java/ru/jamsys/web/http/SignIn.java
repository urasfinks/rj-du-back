package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.promise.PromiseExtension;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.JsonSchema;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.Data;
import ru.jamsys.jt.Device;
import ru.jamsys.jt.User;
import ru.jamsys.promise.repository.AuthRepository;

import java.util.List;
import java.util.Map;

/*
 * Генерация кода для авторизации. Если нет пользователя -> insert. Отправляем код на указанную почту.
 * */
@Component
@RequestMapping
public class SignIn implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public SignIn(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1000L)
                .extension(PromiseExtension::thenSelectUuidDeviceRequire)
                .then("init", (_, promise) -> {
                    //{"mail":"urasfinks@yandex.ru", "code": 123456}
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    String data = servletHandler.getRequestReader().getData();
                    JsonSchema.validate(data, UtilFileResource.getAsString("schema/http/SignIn.json"), "SignIn.json");
                    Map<String, Object> map = UtilJson.getMapOrThrow(data);
                    promise.setRepositoryMap("mail", map.get("mail"));
                    Integer code = (Integer) map.get("code");
                    promise.setRepositoryMap("code", code);
                })
                .thenWithResource("db", JdbcResource.class, "default", (_, promise, jdbcResource) -> {
                    Integer code = promise.getRepositoryMap("code", Integer.class);
                    String mail = promise.getRepositoryMap("mail", String.class);
                    boolean isAppleReviewAppStore = mail.equals("admin@admin.ru") && code == 214365;
                    //Apple Review App Store
                    Map<String, Object> arg = promise.getRepositoryMapClass(AuthRepository.class).get()
                            .append("mail", promise.getRepositoryMap("mail", String.class))
                            .append("code", promise.getRepositoryMap("code", Integer.class));

                    List<Map<String, Object>> user = isAppleReviewAppStore
                            ? jdbcResource.execute(new JdbcRequest(User.GET_BY_CODE_APPLE_REVIEW).addArg(arg))
                            : jdbcResource.execute(new JdbcRequest(User.GET_BY_CODE).addArg(arg));
                    if (user.isEmpty()) {
                        throw new RuntimeException("user not found");
                    }
                    arg.put("id_user", user.getFirst().get("id_user"));
                    jdbcResource.execute(new JdbcRequest(User.RESET_CODE).addArg(arg));
                    List<Map<String, Object>> device = jdbcResource.execute(new JdbcRequest(Device.SELECT_BY_UUID).addArg(arg));
                    //Обновим или добавим uuid устройства для пользователя
                    jdbcResource.execute(
                            device.isEmpty()
                                    ? new JdbcRequest(Device.INSERT).addArg(arg)
                                    : new JdbcRequest(Device.UPDATE).addArg(arg)
                    );
                    // Если всё прошло успешно, обновим не привязанные сокетные данные к персоне, что бы не потерять данные
                    // при разлогинивание, когда происходит перезапись uuid устройства
                    jdbcResource.execute(new JdbcRequest(Data.UPDATE_ID_USER_BEFORE_SIGN_IN).addArg(arg));
                })
                .extension(PromiseExtension::addTerminal);
    }

}
