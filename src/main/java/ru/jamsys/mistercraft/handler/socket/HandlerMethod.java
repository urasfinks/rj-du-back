package ru.jamsys.mistercraft.handler.socket;

import ru.jamsys.mistercraft.socket.Request;

public enum HandlerMethod {

    BROADCAST(new Broadcast()), // Рассылка уведомлений клиентам
    SUBSCRIBE(new Subscribe()), // Подписаться на изменения по uuid_data
    UNSUBSCRIBE(new Unsubscribe()); // Отписаться на изменения по uuid_data

    final SocketHandler socketHandler;

    HandlerMethod(SocketHandler socketHandler) {
        this.socketHandler = socketHandler;
    }

    public void exec(Request request) {
        socketHandler.handler(request);
    }
}
