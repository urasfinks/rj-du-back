package ru.jamsys.mistercraft.handler.http;

import ru.jamsys.*;
import ru.jamsys.mistercraft.UserSessionInfo;
import ru.jamsys.mistercraft.jt.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SocketUpdate implements HttpHandler {
    @Override
    public void handler(JsonHttpResponse jRet, UserSessionInfo userSessionInfo) {
        // uuid_data - точно должен прийти на реальные данные
        // Теоретически существует uuid_data и parent_uuid_data - почитайте DataType.java как это сопоставляется с сокетными данными

        @SuppressWarnings("unchecked")
        Map<String, Object> req = (Map<String, Object>) jRet.getData().get("request");
        @SuppressWarnings("unchecked")
        Map<String, Object> requestData = (Map<String, Object>) req.get("data");
        Map<String, Object> merge = new HashMap<>();

        List<Map<String, Object>> listData = null;

        if (jRet.isStatus()) { //Делаем выборку из БД
            Map<String, Object> arguments = App.jdbcTemplate.createArguments();
            arguments.put("uuid_data", req.get("uuid_data"));
            try {
                userSessionInfo.appendAuthJdbcTemplateArguments(arguments);
                listData = App.jdbcTemplate.exec(App.postgreSQLPoolName, Data.GET_MY_SOCKET, arguments);
                if (listData == null || listData.size() == 0) {
                    jRet.addException("Сокет не найден");
                } else if (listData.size() > 1) {
                    jRet.addException("Выборка дала более одного результата при поиске сокета");
                }
            } catch (Exception e) {
                e.printStackTrace();
                jRet.addException(e);
            }
        }

        String dbValue = null;

        if (jRet.isStatus() && listData != null) { //Проверяем, что данные в БД не пустые
            dbValue = (String) listData.get(0).get("value_data");
            if (dbValue == null || "".equals(dbValue.trim())) {
                jRet.addException("Данные по сокету пусты");
            }
        }

        if (jRet.isStatus()) { //Парсим данные из БД
            WrapJsonToObject<Map<String, Object>> mapWrapJsonToObject = UtilJson.toMap(dbValue);
            if (mapWrapJsonToObject.getException() == null) {
                merge.putAll(mapWrapJsonToObject.getObject());
            } else {
                jRet.addException(mapWrapJsonToObject.getException());
            }
        }

        if (jRet.isStatus() && listData != null) { //Мержим с входящими данными
            merge.putAll(requestData);
            try {
                Map<String, Object> arguments = App.jdbcTemplate.createArguments();
                arguments.put("value_data", UtilJson.toStringPretty(merge, "{}"));
                arguments.put("id_data", listData.get(0).get("id_data"));
                App.jdbcTemplate.exec(App.postgreSQLPoolName, Data.UPDATE_MY_SOCKET, arguments);
            } catch (Exception e) {
                jRet.addException(e);
            }
        }

    }
}
