package ru.jamsys.mistercraft.handler.http.socket_insert;

import java.util.Map;

public interface Handler {
    void handler(Map<String, Object> args, Map<String, Object> requestData) throws Exception;
}
