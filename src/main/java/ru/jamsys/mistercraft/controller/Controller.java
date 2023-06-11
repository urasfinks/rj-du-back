package ru.jamsys.mistercraft.controller;

import jakarta.annotation.Nullable;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.mistercraft.UserSessionInfo;

public interface Controller {
    void handler(JsonHttpResponse jRet, @Nullable UserSessionInfo userSessionInfo);
}
