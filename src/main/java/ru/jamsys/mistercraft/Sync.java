package ru.jamsys.mistercraft;

import ru.jamsys.App;
import ru.jamsys.Util;
import ru.jamsys.mistercraft.jt.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sync {

    public static String handler(UserSessionInfo userSessionInfo, Map<String, Object> parsedJson) {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        /*Правила:
         * 1) Обновляем содержимое (к нам приходят пользовательские данные с обнулёнными revision)
         * 2) Селектим по ревизиям (так как на обновлении установятся новые revision, то мы как зеркалом будем возвращать новые номера ревизий для данных)
         *
         * Пока пусть будет режим бумеранга, если дальше будет медленно работать - будем думать и оптимизировать, пока так.
         * */
        //Блок обновления
        try {
            //System.out.println(parsedJson);
            /*
           {
            "maxRevisionByType": {
              "js": 0,
              "socket": 0,
              "systemData": 0,
              "template": 0,
              "userDataRSync": 0,
              "any": 0
            },
            "userData": [
              {
                "id_data": 26,
                "uuid_data": "500efb3e-8dfe-4b77-a536-9ea866a1ffe4",
                "value_data": "{\"label\":\"Хьюмидор\"}",
                "type_data": "userDataRSync",
                "parent_uuid_data": null,
                "key_data": "humidor",
                "date_add_data": 1685559500427,
                "date_update_data": null,
                "revision_data": 0,
                "is_remove_data": 0
              }
            ],
            "socketData": [
              {
                "id_data": 27,
                "uuid_data": "test",
                "value_data": "{}",
                "type_data": "socket",
                "parent_uuid_data": null,
                "key_data": null,
                "date_add_data": 1685559656447,
                "date_update_data": null,
                "revision_data": 0,
                "is_remove_data": 0
              },
              {
                "id_data": 28,
                "uuid_data": "test2",
                "value_data": "{}",
                "type_data": "socket",
                "parent_uuid_data": "test",
                "key_data": null,
                "date_add_data": 1685559656448,
                "date_update_data": null,
                "revision_data": 0,
                "is_remove_data": 0
              }
            ]
          }
             * */
            List<Map> userDatas = (List<Map>) parsedJson.get("userData");
            for(Map userData : userDatas){
                System.out.println(userData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Блок выгрузки
        try {
            @SuppressWarnings("unchecked")
            Map<String, Integer> rqMaxRevisionByType = (Map<String, Integer>) parsedJson.get("maxRevisionByType");
            Map<String, Long> dbMaxRevisionByType = getMaxRevisionByType(userSessionInfo);

            for (DataType dataType : DataType.values()) {
                int rqRevision = rqMaxRevisionByType.getOrDefault(dataType.toString(), 0);
                long dbRevision = dbMaxRevisionByType.getOrDefault(dataType.toString(), 0L);
                if (dbRevision > rqRevision) {
                    Map<String, Object> arguments = App.jdbcTemplate.createArguments();
                    arguments.put("type_data", dataType.toString());
                    arguments.put("revision_data", rqRevision);
                    userSessionInfo.appendAuthJdbcTemplateArguments(arguments);
                    List<Map<String, Object>> exec = new ArrayList<>();
                    switch (dataType) {
                        case js:
                        case any:
                        case systemData:
                        case template:
                            exec = App.jdbcTemplate.exec(App.postgreSQLPoolName, Data.SELECT_SYSTEM_DATA_RANGE, arguments);
                            break;
                        case userDataRSync:
                            exec = App.jdbcTemplate.exec(App.postgreSQLPoolName, Data.SELECT_USER_DATA_RANGE, arguments);
                            break;
                        case socket:
                            exec = App.jdbcTemplate.exec(App.postgreSQLPoolName, Data.SELECT_SOCKET_DATA_RANGE, arguments);
                            break;
                    }
                    result.put(dataType.toString(), exec);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String json = Util.jsonObjectToStringPretty(result);
        System.out.println("Sync.sync() => " + json);
        return json;
    }

    private static Map<String, Long> getMaxRevisionByType(UserSessionInfo userSessionInfo) throws Exception {
        Map<String, Long> dbMapRevision = new HashMap<>();
        Map<String, Object> arguments = App.jdbcTemplate.createArguments();
        userSessionInfo.appendAuthJdbcTemplateArguments(arguments);
        List<Map<String, Object>> exec = App.jdbcTemplate.exec(App.postgreSQLPoolName, Data.SELECT_MAX_REVISION_BY_TYPE, arguments);
        for (Map<String, Object> row : exec) {
            dbMapRevision.put((String) row.get("key"), (Long) row.get("max"));
        }
        return dbMapRevision;
    }
}
