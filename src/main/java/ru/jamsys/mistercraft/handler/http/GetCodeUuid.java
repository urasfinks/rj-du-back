package ru.jamsys.mistercraft.handler.http;

import ru.jamsys.JsonHttpResponse;
import ru.jamsys.mistercraft.UserSessionInfo;

import java.util.Map;

public class GetCodeUuid implements HttpHandler {
    @Override
    public void handler(JsonHttpResponse jRet, UserSessionInfo userSessionInfo) {
        @SuppressWarnings("unchecked")
        Map<String, Object> req = (Map<String, Object>) jRet.getData().get("request");
        Integer code = (Integer) req.get("code");
        if (jRet.isStatus() && code == null) {
            jRet.addException("Код в запросе пустой");
        }
        String dataUuid = GenCodeUuid.codeManager.get(code);
        if (jRet.isStatus() && dataUuid == null) {
            jRet.addException("Код не найден либо его срок истёк");
        }
        if (jRet.isStatus() && dataUuid != null) {
            jRet.addData("code", code);
            jRet.addData("uuid", dataUuid);
        }
    }
}
