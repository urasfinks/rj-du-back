package ru.jamsys.mistercraft;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.jamsys.Util;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SocketHandler extends TextWebSocketHandler {

    public SocketHandler() {
        new Thread(() -> {
            while (true) {
                for (WebSocketSession webSocketSession : sessions) {
                    try {
                        webSocketSession.sendMessage(new TextMessage("Hello!"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Util.sleepMillis(1000);
            }
        }).start();
    }

    List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message)
            throws IOException {
        for (WebSocketSession webSocketSession : sessions) {
            //Map value = new Gson().fromJson(message.getPayload(), Map.class);
            webSocketSession.sendMessage(new TextMessage("Hello!"));
        }
    }

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) {
        System.out.println("Connect: " + session);
        //the messages will be broadcasted to all users.
        sessions.add(session);
    }
}
