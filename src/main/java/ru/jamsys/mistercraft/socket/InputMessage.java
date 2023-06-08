package ru.jamsys.mistercraft.socket;

import lombok.Data;

@Data
public class InputMessage {
    String body;

    public InputMessage(String body) {
        this.body = body;
    }
}
