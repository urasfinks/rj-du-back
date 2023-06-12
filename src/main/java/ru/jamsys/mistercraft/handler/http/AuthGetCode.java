package ru.jamsys.mistercraft.handler.http;

import jakarta.annotation.Nullable;
import ru.jamsys.App;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.Util;
import ru.jamsys.mistercraft.UserSessionInfo;
import ru.jamsys.mistercraft.jt.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthGetCode implements HttpHandler {
    @Override
    public void handler(JsonHttpResponse jRet, @Nullable UserSessionInfo userSessionInfo) {
        //{"login":"urasfinks@yandex.ru"}
        Map<String, Object> req = new HashMap<>();
        if (jRet.isStatus()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> req2 = (Map<String, Object>) jRet.getData().get("request");
                req = req2;
            } catch (Exception e) {
                jRet.addException(e);
            }
        }
        Map<String, Object> arguments = null;
        if (jRet.isStatus()) {
            try {
                arguments = App.jdbcTemplate.createArguments();
                arguments.put("mail", req.get("mail"));
                arguments.put("code", Util.random(10000, 99999));
                List<Map<String, Object>> user = App.jdbcTemplate.exec(App.postgreSQLPoolName, User.GET_BY_MAIL, req);
                if (user.size() == 0) {
                    App.jdbcTemplate.exec(App.postgreSQLPoolName, User.INSERT, arguments);
                } else {
                    App.jdbcTemplate.exec(App.postgreSQLPoolName, User.SET_CODE, arguments);
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
