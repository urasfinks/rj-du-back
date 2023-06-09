package ru.jamsys.mistercraft.socket.invoke;

import ru.jamsys.mistercraft.socket.Request;

public class Update implements Invoke {

    @Override
    public void exec(Request request) {
        System.out.println("Hello iam update");
    }

}
