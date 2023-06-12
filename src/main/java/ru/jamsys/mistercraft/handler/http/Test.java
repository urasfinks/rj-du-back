package ru.jamsys.mistercraft.handler.http;

import ru.jamsys.JsonHttpResponse;
import ru.jamsys.mistercraft.UserSessionInfo;

public class Test implements HttpHandler {
    @Override
    public void handler(JsonHttpResponse jRet, UserSessionInfo userSessionInfo) {
        jRet.addData("response", "Hello world");
    }
}
