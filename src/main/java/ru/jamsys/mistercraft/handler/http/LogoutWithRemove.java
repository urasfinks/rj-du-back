package ru.jamsys.mistercraft.handler.http;

import ru.jamsys.App;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.mistercraft.UserSessionInfo;
import ru.jamsys.mistercraft.jt.Data;

import java.util.Map;

public class LogoutWithRemove implements HttpHandler {
    @Override
    public void handler(JsonHttpResponse jRet, UserSessionInfo userSessionInfo) {
        try {
            Map<String, Object> arguments = App.jdbcTemplate.createArguments();
            userSessionInfo.appendAuthJdbcTemplateArguments(arguments);
            App.jdbcTemplate.execute(App.postgresqlPoolName, Data.REMOVE_ALL, arguments);
        } catch (Exception e) {
            jRet.addException(e);
        }
    }
}
