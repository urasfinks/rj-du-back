package ru.jamsys.mistercraft;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.jamsys.*;
import ru.jamsys.component.Broker;
import ru.jamsys.component.ThreadBalancerFactory;
import ru.jamsys.mistercraft.socket.RequestMessage;
import ru.jamsys.mistercraft.socket.SessionWrap;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ControllerWebSocket extends TextWebSocketHandler {

    public static String nameSocketRequestReader = "SocketRequestReader";

    Map<WebSocketSession, SessionWrap> mapConnection = new ConcurrentHashMap<>();

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        RequestMessage requestMessage = new RequestMessage();
        requestMessage.setBody(message.getPayload());
        requestMessage.setSessionWrap(mapConnection.get(session));
        App.context.getBean(Broker.class).add(RequestMessage.class, requestMessage);
        App.context.getBean(ThreadBalancerFactory.class).getThreadBalancer(nameSocketRequestReader).wakeUp();
    }

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        InetSocketAddress remoteAddress = session.getRemoteAddress();
        if (remoteAddress != null) {
            String host = session.getRemoteAddress().getHostString();
            if (!checkCountConnectionPerHost(host)) {
                Util.logConsole("Connection[" + mapConnection.size() + "]; afterConnectionEstablished Overload host connection: " + session);
                session.close(CloseStatus.SERVICE_OVERLOAD);
                return;
            }
            URI uri = session.getUri();
            if (uri != null) {
                String path = uri.getPath();
                if (path != null && path.startsWith("/") && path.length() == 37) { //Ждём uuid устройства
                    Util.logConsole("Connection[" + mapConnection.size() + "]; afterConnectionEstablished Success: " + session);
                    UserSessionInfo userSessionInfo = new UserSessionInfo();
                    userSessionInfo.setDeviceUuid(path.substring(1));
                    mapConnection.put(session, new SessionWrap(session, userSessionInfo, host));
                } else {
                    Util.logConsole("Connection[" + mapConnection.size() + "]; afterConnectionEstablished Error: " + session);
                    session.close(CloseStatus.POLICY_VIOLATION);
                }
            } else {
                Util.logConsole("Connection[" + mapConnection.size() + "]; afterConnectionEstablished Error read uri: " + session);
                session.close(CloseStatus.BAD_DATA);
            }
        } else {
            Util.logConsole("Connection[" + mapConnection.size() + "]; afterConnectionEstablished Error read address: " + session);
            session.close(CloseStatus.BAD_DATA);
        }
    }

    private boolean checkCountConnectionPerHost(String host) {
        int countConnectionPerHost = 0;
        WebSocketSession[] webSocketSessions = mapConnection.keySet().toArray(new WebSocketSession[0]);
        for (WebSocketSession webSocketSession : webSocketSessions) {
            SessionWrap sessionWrap = mapConnection.get(webSocketSession);
            if (sessionWrap != null) {
                if (sessionWrap.getRemoteAddress().equals(host)) {
                    countConnectionPerHost++;
                }
                if (countConnectionPerHost > 5) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        Util.logConsole("Connection[" + mapConnection.size() + "]; afterConnectionClosed: " + session + "; status: " + status);
        mapConnection.remove(session);
    }

    private void send(@NotNull WebSocketSession session, String data) {
        try {
            session.sendMessage(new TextMessage(data));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
