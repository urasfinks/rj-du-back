package ru.jamsys.mistercraft.handler.http.socket_insert;

import ru.jamsys.Util;

import java.util.Map;

public class TimestampHandlerImpl implements Handler {
    @Override
    public void handler(Map<String, Object> args, Map<String, Object> requestData) throws Exception {
        requestData.put((String) args.get("field"), Util.getTimestamp());
    }
}
