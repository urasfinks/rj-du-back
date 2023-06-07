package ru.jamsys;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import ru.jamsys.mistercraft.SocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new SocketHandler(), "*");
    }
}