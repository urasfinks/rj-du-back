package ru.jamsys.mistercraft.socket.invoke;

import ru.jamsys.mistercraft.socket.Request;
import ru.jamsys.mistercraft.socket.SessionWrap;

public class Unsubscribe implements Invoke{

    @Override
    public void exec(Request request) {
        SessionWrap sessionWrap = request.getRequestMessage().getSessionWrap();
        if (sessionWrap != null) {
            sessionWrap.unsubscribe(request.getUuidData());
        }
    }

}
