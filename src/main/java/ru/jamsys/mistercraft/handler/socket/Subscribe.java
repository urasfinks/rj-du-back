package ru.jamsys.mistercraft.handler.socket;

import ru.jamsys.mistercraft.socket.Request;
import ru.jamsys.mistercraft.socket.SessionWrap;

// Использование в Web
// У Веб страницы нет БД и нет привычной синхронизации данных, также нет uuid_device
// При этом данные надо будет как-то получать своевременно
public class Subscribe implements SocketHandler {

    @Override
    public void handler(Request request) {
        SessionWrap sessionWrap = request.getRequestMessage().getSessionWrap();
        if (sessionWrap != null) {
            sessionWrap.subscribe(request.getUuidData());
        }
    }

}
