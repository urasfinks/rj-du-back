package ru.jamsys.mistercraft.socket;

import lombok.Data;
import org.springframework.web.socket.WebSocketSession;
import ru.jamsys.mistercraft.UserSessionInfo;

@Data
public class WebSocketSessionWrap {
    WebSocketSession session;
    UserSessionInfo userSessionInfo;

    String remoteAddress;

    public WebSocketSessionWrap(WebSocketSession session, UserSessionInfo userSessionInfo, String remoteAddress) {
        this.session = session;
        this.userSessionInfo = userSessionInfo;
        this.remoteAddress = remoteAddress;
    }
}
