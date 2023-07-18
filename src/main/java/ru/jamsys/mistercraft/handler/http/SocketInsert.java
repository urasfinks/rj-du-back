package ru.jamsys.mistercraft.handler.http;

import ru.jamsys.App;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.component.Broker;
import ru.jamsys.component.ThreadBalancerFactory;
import ru.jamsys.mistercraft.UserSessionInfo;
import ru.jamsys.mistercraft.handler.http.socket_insert.HandlerMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SocketInsert implements HttpHandler {

    final Broker broker;
    final ThreadBalancerFactory threadBalancerFactory;

    public SocketInsert() {
        this.broker = App.context.getBean(Broker.class);
        threadBalancerFactory = App.context.getBean(ThreadBalancerFactory.class);
    }

    @Override
    public void handler(JsonHttpResponse jRet, UserSessionInfo userSessionInfo) {

        Map<String, Object> request = (Map<String, Object>) jRet.getData().get("request");
        HashMap<String, Object> requestData = new HashMap<>();

        List<Map<String, Object>> actions = (List<Map<String, Object>>) request.get("actions");
        for (Map<String, Object> action : actions) {
            String method = (String) action.get("action");
            HandlerMethod[] values = HandlerMethod.values();

            Map<String, Object> args = (Map<String, Object>) action.get("arguments");

            if (containsMethod(method.toUpperCase())) {
                HandlerMethod handlerMethod = HandlerMethod.valueOf(method.toUpperCase());
                if (!handlerMethod.validateArguments(args, jRet)) {
                    break;
                }
                try {
                    handlerMethod.get().handler(args, requestData);
                } catch (Exception e) {
                    jRet.addException(e);
                }
            } else {
                jRet.addException("Action " + method + " does not exist");
            }
            if (!jRet.isStatus()) {
                break;
            }
        }

        request.put("data", requestData);

        HttpHandler httpHandler = ru.jamsys.mistercraft.handler.http.HandlerMethod.SOCKET_UPDATE.get();
        httpHandler.handler(jRet, userSessionInfo);
    }

    public static boolean containsMethod(String test) {
        for (HandlerMethod c : HandlerMethod.values()) {
            if (c.name().equals(test)) {
                return true;
            }
        }
        return false;
    }

}

