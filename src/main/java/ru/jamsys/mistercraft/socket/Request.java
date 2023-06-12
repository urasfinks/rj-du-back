package ru.jamsys.mistercraft.socket;

import lombok.Data;
import ru.jamsys.mistercraft.handler.socket.HandlerMethod;

import java.util.Map;

@Data
public class Request {

    HandlerMethod handlerMethod;
    String uuidData;
    Map<String, Object> data;
    RequestMessage requestMessage;

    public void exec() {
        handlerMethod.exec(this);
    }

}
