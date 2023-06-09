package ru.jamsys.mistercraft.socket;

import ru.jamsys.mistercraft.socket.invoke.Invoke;
import ru.jamsys.mistercraft.socket.invoke.Subscribe;
import ru.jamsys.mistercraft.socket.invoke.Unsubscribe;
import ru.jamsys.mistercraft.socket.invoke.Update;

public enum Operation {
    SUBSCRIBE(new Subscribe()), //Подписаться на изменения по uuid_data
    UNSUBSCRIBE(new Unsubscribe()), //Отписаться на изменения по uuid_data
    UPDATE(new Update()); //Изменить точечно данные по uuid_data

    final Invoke invoke;

    Operation(Invoke invoke) {
        this.invoke = invoke;
    }

    public void exec(Request request) {
        invoke.exec(request);
    }
}
