package ru.jamsys.mistercraft.socket;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.jamsys.App;
import ru.jamsys.Util;
import ru.jamsys.UtilJson;
import ru.jamsys.component.Broker;
import ru.jamsys.component.ThreadBalancerFactory;
import ru.jamsys.mistercraft.UserSessionInfo;
import ru.jamsys.thread.balancer.ThreadBalancer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class Handler extends TextWebSocketHandler {

    List<WebSocketSessionWrap> listConnection = new CopyOnWriteArrayList<>();

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) throws Exception {
        super.handleTextMessage(session, message);

        App.context.getBean(Broker.class).add(InputMessage.class, new InputMessage(message.getPayload()));
        ThreadBalancer threadBalancer = App.context.getBean(ThreadBalancerFactory.class).getThreadBalancer(App.nameSocketInputReader);
        threadBalancer.wakeUpIfEveryoneIsSleeping();
    }

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        InetSocketAddress remoteAddress = session.getRemoteAddress();
        if (remoteAddress != null) {
            String host = session.getRemoteAddress().getHostString();
            int countConnectionPerHost = 0;
            WebSocketSessionWrap[] webSocketSessionWraps = listConnection.toArray(new WebSocketSessionWrap[0]);
            for (WebSocketSessionWrap webSocketSessionWrap : webSocketSessionWraps) {
                if (webSocketSessionWrap.getRemoteAddress().equals(host)) {
                    countConnectionPerHost++;
                }
                if (countConnectionPerHost > 5) {
                    Util.logConsole(Thread.currentThread(), "Connection[" + listConnection.size() + "]; afterConnectionEstablished Overload host connection: " + session);
                    session.close(CloseStatus.SERVICE_OVERLOAD);
                    break;
                }
            }
            URI uri = session.getUri();
            if (uri != null) {
                String path = uri.getPath();
                if (path != null && path.startsWith("/") && path.length() == 37) { //Ждём uuid устройства
                    Util.logConsole(Thread.currentThread(), "Connection[" + listConnection.size() + "]; afterConnectionEstablished Success: " + session);
                    UserSessionInfo userSessionInfo = new UserSessionInfo();
                    userSessionInfo.setDeviceUuid(path.substring(1));
                    listConnection.add(new WebSocketSessionWrap(session, userSessionInfo, host));
                } else {
                    Util.logConsole(Thread.currentThread(), "Connection[" + listConnection.size() + "]; afterConnectionEstablished Error: " + session);
                    session.close(CloseStatus.POLICY_VIOLATION);
                }
            } else {
                Util.logConsole(Thread.currentThread(), "Connection[" + listConnection.size() + "]; afterConnectionEstablished Error read uri: " + session);
                session.close(CloseStatus.BAD_DATA);
            }
        } else {
            Util.logConsole(Thread.currentThread(), "Connection[" + listConnection.size() + "]; afterConnectionEstablished Error read address: " + session);
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        Util.logConsole(Thread.currentThread(), "Connection[" + listConnection.size() + "]; afterConnectionClosed: " + session + "; status: " + status);
        WebSocketSessionWrap[] webSocketSessionWraps = listConnection.toArray(new WebSocketSessionWrap[0]);
        for (WebSocketSessionWrap webSocketSessionWrap : webSocketSessionWraps) {
            if (webSocketSessionWrap.getSession().equals(session)) {
                listConnection.remove(webSocketSessionWrap);
                break;
            }
        }
    }

    @SuppressWarnings("unused")
    public void send(Set<String> listDeviceUuid, Map<String, Object> data) {
        String sendData = UtilJson.toString(data, "{}");
        WebSocketSessionWrap[] webSocketSessionWraps = listConnection.toArray(new WebSocketSessionWrap[0]);
        for (WebSocketSessionWrap webSocketSessionWrap : webSocketSessionWraps) {
            if (listDeviceUuid.contains(webSocketSessionWrap.getUserSessionInfo().getDeviceUuid())) {
                send(webSocketSessionWrap.getSession(), sendData);
            }
        }
    }

    private void send(@NotNull WebSocketSession session, String data) {
        try {
            session.sendMessage(new TextMessage(data));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
