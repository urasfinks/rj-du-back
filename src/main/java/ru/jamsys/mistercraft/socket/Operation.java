package ru.jamsys.mistercraft.socket;

import ru.jamsys.mistercraft.handler.socket.SocketHandler;
import ru.jamsys.mistercraft.handler.socket.Subscribe;
import ru.jamsys.mistercraft.handler.socket.Unsubscribe;

public enum Operation {
    SUBSCRIBE(new Subscribe()), //Подписаться на изменения по uuid_data
    UNSUBSCRIBE(new Unsubscribe()); //Отписаться на изменения по uuid_data

    final SocketHandler socketHandler;

    Operation(SocketHandler socketHandler) {
        this.socketHandler = socketHandler;
    }

    public void exec(Request request) {
        socketHandler.handler(request);
    }
}
