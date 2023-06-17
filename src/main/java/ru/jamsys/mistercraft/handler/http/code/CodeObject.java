package ru.jamsys.mistercraft.handler.http.code;

import lombok.Data;

@Data
public class CodeObject {

    long timestampAdd = System.currentTimeMillis();
    String uuid = java.util.UUID.randomUUID().toString();
    int code;

}
