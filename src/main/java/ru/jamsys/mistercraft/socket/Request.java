package ru.jamsys.mistercraft.socket;

import lombok.Data;

import java.util.Map;

@Data
public class Request {

    Operation operation;
    String uuidData;
    Map<String, Object> data;
    RequestMessage requestMessage;

    public void exec() {
        operation.exec(this);
    }

}
