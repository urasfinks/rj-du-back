package ru.jamsys.mistercraft.controller;

import ru.jamsys.JsonHttpResponse;
import ru.jamsys.mistercraft.UserSessionInfo;

public class SocketUpdate implements Controller{
    @Override
    public void handler(JsonHttpResponse jRet, UserSessionInfo userSessionInfo) {
        // uuid_data - точно должен прийти на реальные данные
        // Теоретически существует uuid_data и parent_uuid_data - почитайте DataType.java как это сопоставляется с сокетными данными
        jRet.addData("hello", "world");
    }
}
