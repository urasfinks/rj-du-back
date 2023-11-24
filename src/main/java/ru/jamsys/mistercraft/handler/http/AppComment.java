package ru.jamsys.mistercraft.handler.http;

import org.springframework.beans.factory.annotation.Configurable;
import ru.jamsys.App;
import ru.jamsys.Configuration;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.UtilJson;
import ru.jamsys.component.Telegram;
import ru.jamsys.mistercraft.UserSessionInfo;

@Configurable
public class AppComment implements HttpHandler {

    @Override
    public void handler(JsonHttpResponse jRet, UserSessionInfo userSessionInfo) {
        Configuration configuration = App.context.getBean(Configuration.class);
        Telegram telegram = App.context.getBean(Telegram.class);
        telegram.syncSend(configuration.getTelegramIdChat(), UtilJson.toString(jRet.getData().get("request"), "{}"), jRet);
    }
}
