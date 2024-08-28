package ru.jamsys.web.http.sub.handler;

import java.util.Map;

public interface SubHandler {

    void handler(Map<String, Object> args, Map<String, Object> requestData) throws Throwable;

}
