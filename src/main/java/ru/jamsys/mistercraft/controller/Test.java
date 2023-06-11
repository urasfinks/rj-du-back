package ru.jamsys.mistercraft.controller;

import ru.jamsys.JsonHttpResponse;
import ru.jamsys.mistercraft.UserSessionInfo;

public class Test implements Controller{
    @Override
    public void handler(JsonHttpResponse jRet, UserSessionInfo userSessionInfo) {
        jRet.addData("response", "Hello world");
    }
}
