package ru.jamsys.mistercraft.handler.http;

import ru.jamsys.App;
import ru.jamsys.Configuration;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.UtilJson;
import ru.jamsys.component.ReCaptcha;
import ru.jamsys.component.Telegram;
import ru.jamsys.mistercraft.UserSessionInfo;

import java.util.Map;

public class Comment implements HttpHandler {
    @Override
    public void handler(JsonHttpResponse jRet, UserSessionInfo userSessionInfo) {
        Map<String, Object> req = (Map<String, Object>) jRet.getData().get("request");
        final String gRecaptchaResponse = req.containsKey("g-recaptcha-response") ? (String) req.get("g-recaptcha-response") : null;
        if (gRecaptchaResponse == null) {
            jRet.addException("g-recaptcha-response пустое");
        }
        if (jRet.isStatus()) {
            ReCaptcha reCaptcha = App.context.getBean(ReCaptcha.class);
            reCaptcha.isValid(gRecaptchaResponse, jRet);
        }
        if (jRet.isStatus()) {
            Telegram telegram = App.context.getBean(Telegram.class);
            req.remove("g-recaptcha-response");
            Configuration configuration = App.context.getBean(Configuration.class);
            telegram.syncSend(configuration.getTelegramIdChat(), UtilJson.toString(req, "{}"), jRet);
        }
    }
}
