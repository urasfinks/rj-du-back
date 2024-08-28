package ru.jamsys;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import ru.jamsys.core.component.web.socket.WebSocketCheckConnection;

@Component
public class WebSocketCheckConnectionImpl implements WebSocketCheckConnection {

    public boolean check(@NotNull WebSocketSession webSocketSession) {
        System.out.println("Connect");
        return true;
    }

}
