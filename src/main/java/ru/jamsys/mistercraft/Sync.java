package ru.jamsys.mistercraft;

import ru.jamsys.App;
import ru.jamsys.Util;
import ru.jamsys.mistercraft.jt.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sync {

    public static String handler(UserSessionInfo userSessionInfo, Map<String, Object> parsedJson) {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        System.out.println("Sync.sync() data: " + parsedJson);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Integer> requestMapRevision = (Map<String, Integer>) parsedJson.get("maxRevisionByType");
            Map<String, Long> dbMapRevision = getDbRevision(userSessionInfo);

            for (DataType dataType : DataType.values()) {
                //long deviceRevision = requestMapRevision.containsKey(dataType.toString()) ? requestMapRevision.get(dataType.toString()) : 0L;
                int deviceRevision = requestMapRevision.getOrDefault(dataType.toString(), 0);
                long dbRevision = dbMapRevision.getOrDefault(dataType.toString(), 0L);
                if (dbRevision > deviceRevision) {
                    Map<String, Object> arguments = App.jdbcTemplate.createArguments();
                    arguments.put("type_data", dataType.toString());
                    arguments.put("revision_data", deviceRevision);
                    arguments.put("id_user", userSessionInfo.getIdUser());
                    List<Map<String, Object>> exec = App.jdbcTemplate.exec(App.postgreSQLPoolName, dataType.isUserData() ? Data.SELECT_USER_DATA_RANGE : Data.SELECT_SHARE_DATA_RANGE, arguments);
                    //System.out.println(exec);
                    result.put(dataType.toString(), exec);
                    //System.out.println(dataType + " deviceRevision: " + deviceRevision + "; dbRevision: " + dbRevision);
                }
            }
            //System.out.println(exec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String json = Util.jsonObjectToStringPretty(result);
        System.out.println("Sync.sync() => " + json);
        return json;
    }

    private static Map<String, Long> getDbRevision(UserSessionInfo userSessionInfo) throws Exception {
        Map<String, Long> dbMapRevision = new HashMap<>();


        Map<String, Object> arguments = App.jdbcTemplate.createArguments();
        List<Map<String, Object>> exec = App.jdbcTemplate.exec(App.postgreSQLPoolName, Data.SELECT_SHARE_MAX_REVISION_BY_TYPE, arguments);

        if (userSessionInfo.isRegister()) {
            arguments.put("id_user", userSessionInfo.getIdUser());
            List<Map<String, Object>> exec2 = App.jdbcTemplate.exec(App.postgreSQLPoolName, Data.SELECT_USER_MAX_REVISION_BY_TYPE, arguments);
            if (exec2.size() > 0) {
                exec.addAll(exec2);
            }
        }
        for (Map<String, Object> row : exec) {
            dbMapRevision.put((String) row.get("type_data"), (Long) row.get("max"));
        }
        return dbMapRevision;
    }
}
