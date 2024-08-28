package ru.jamsys.web.http.sub.handler;

import ru.jamsys.core.flat.util.Util;

import java.util.Map;

public class TimestampSubHandlerImpl implements SubHandler {

    @Override
    public void handler(Map<String, Object> args, Map<String, Object> requestData) throws Exception {
        requestData.put((String) args.get("field"), Util.getTimestamp());
    }

}
