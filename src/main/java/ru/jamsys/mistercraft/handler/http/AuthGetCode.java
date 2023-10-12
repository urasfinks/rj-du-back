package ru.jamsys.mistercraft.handler.http;

import jakarta.annotation.Nullable;
import ru.jamsys.App;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.Util;
import ru.jamsys.mistercraft.UserSessionInfo;
import ru.jamsys.mistercraft.jt.User;

import java.util.List;
import java.util.Map;

public class AuthGetCode implements HttpHandler {
    @Override
    public void handler(JsonHttpResponse jRet, @Nullable UserSessionInfo userSessionInfo) {
        //{"mail":"urasfinks@yandex.ru"}
        @SuppressWarnings("unchecked")
        Map<String, Object> req = (Map<String, Object>) jRet.getData().get("request");
        String mail = (String) req.get("mail");
        if (!mail.equals("admin@admin.ru")) { //Apple Review App Store
            Map<String, Object> arguments = null;
            if (jRet.isStatus()) {
                try {
                    arguments = App.jdbcTemplate.createArguments();
                    arguments.put("mail", req.get("mail"));
                    arguments.put("code", Util.random(100000, 999999));
                    List<Map<String, Object>> user = App.jdbcTemplate.execute(App.postgresqlPoolName, User.GET_BY_MAIL, req);
                    if (user.size() == 0) {
                        App.jdbcTemplate.execute(App.postgresqlPoolName, User.INSERT, arguments);
                    } else {
                        App.jdbcTemplate.execute(App.postgresqlPoolName, User.SET_CODE, arguments);
                    }
                } catch (Exception e) {
                    jRet.addException(e);
                }
            }
            if (jRet.isStatus() && arguments != null) {
                try {
                    App.eMail.sendCode((String) arguments.get("mail"), arguments.get("code").toString());
                } catch (Exception e) {
                    jRet.addException(e);
                }
            }
        }
    }
}
