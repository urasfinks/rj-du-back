package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.PromiseExtension;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.http.HttpAsyncResponse;
import ru.jamsys.core.flat.util.JsonSchema;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.web.http.sub.handler.SubHandlerMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Генерация кода для авторизации. Если нет пользователя -> insert. Отправляем код на указанную почту.
 * */
@Component
@RequestMapping
public class SocketExtend implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public SocketExtend(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1000L)
                .extension(PromiseExtension::thenSelectIdUser)
                .then("init", (_, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    String data = input.getHttpRequestReader().getData();
                    JsonSchema.validate(data, UtilFileResource.getAsString("schema/http/InsertSocketData.json"), "InsertSocketData.json");
                    Map<String, Object> map = UtilJson.getMapOrThrow(data);
                    HashMap<String, Object> requestData = new HashMap<>();

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> actions = (List<Map<String, Object>>) map.get("actions");
                    for (Map<String, Object> action : actions) {
                        String method = (String) action.get("action");
                        if (containsMethod(method.toUpperCase())) {
                            SubHandlerMethod handlerMethod = SubHandlerMethod.valueOf(method.toUpperCase());
                            @SuppressWarnings("unchecked")
                            Map<String, Object> args = (Map<String, Object>) action.get("arguments");
                            handlerMethod.validateOrThrow(args);
                            handlerMethod.get().handler(args, requestData);
                        } else {
                            throw new RuntimeException("Action " + method + " does not exist");
                        }
                    }
                    System.out.println(requestData);
                })
                .extension(PromiseExtension::addTerminal);
    }

    public static boolean containsMethod(String test) {
        for (SubHandlerMethod c : SubHandlerMethod.values()) {
            if (c.name().equals(test)) {
                return true;
            }
        }
        return false;
    }

}
