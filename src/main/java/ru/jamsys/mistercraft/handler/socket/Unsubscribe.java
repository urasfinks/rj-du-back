package ru.jamsys.mistercraft.handler.socket;

import ru.jamsys.mistercraft.socket.Request;
import ru.jamsys.mistercraft.socket.SessionWrap;

public class Unsubscribe implements SocketHandler {

    @Override
    public void handler(Request request) {
        SessionWrap sessionWrap = request.getRequestMessage().getSessionWrap();
        if (sessionWrap != null) {
            sessionWrap.unsubscribe(request.getUuidData());
        }
    }

}
