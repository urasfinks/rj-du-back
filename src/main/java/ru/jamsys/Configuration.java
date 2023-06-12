package ru.jamsys;

import org.springframework.web.socket.config.annotation.*;
import ru.jamsys.mistercraft.ControllerWebSocket;

@org.springframework.context.annotation.Configuration
@EnableWebSocket
public class Configuration implements WebSocketConfigurer {
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ControllerWebSocket(), "*");
    }
}