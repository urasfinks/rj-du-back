package ru.jamsys.mistercraft.handler.http;

import ru.jamsys.App;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.mistercraft.UserSessionInfo;

import java.util.List;
import java.util.Map;

public class Data implements HttpHandler {
    @Override
    public void handler(JsonHttpResponse jRet, UserSessionInfo userSessionInfo) {
        try {
            Map<String, Object> parsedJson = (Map<String, Object>) jRet.getData().get("request");
            String dataUuid = (String) parsedJson.get("uuid");
            if (jRet.isStatus()) {
                if (dataUuid == null || "".equals(dataUuid.trim())) {
                    jRet.addException("Пустой uuid");
                }
            }
            if (jRet.isStatus()) {
                if (!GenCodeUuid.dataManager.get().containsKey(dataUuid)) {
                    jRet.addException("Нет данных в кеше");
                }
            }
            if (jRet.isStatus()) {
                Map<String, Object> arguments = App.jdbcTemplate.createArguments();
                arguments.put("uuid_data", dataUuid);
                List<Map<String, Object>> execute = App.jdbcTemplate.execute(App.postgresqlPoolName, ru.jamsys.mistercraft.jt.Data.SELECT, arguments);
                if (!execute.isEmpty()) {
                    jRet.addData("data", execute.get(0).get("value_data"));
                } else {
                    jRet.addException("Данные не найдены");
                }
            }
        } catch (Exception e) {
            jRet.addException(e);
        }
    }
}
