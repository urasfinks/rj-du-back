package ru.jamsys.mistercraft.handler.http;

import jakarta.annotation.Nullable;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.mistercraft.UserSessionInfo;

public interface HttpHandler {
    void handler(JsonHttpResponse jRet, @Nullable UserSessionInfo userSessionInfo);
}
