package ru.jamsys.mistercraft;

import ru.jamsys.App;
import ru.jamsys.Util;
import ru.jamsys.UtilJson;
import ru.jamsys.mistercraft.jt.Data;
import ru.jamsys.mistercraft.jt.DataByParent;

import java.util.*;

public class Sync {

    public static String handler(UserSessionInfo userSessionInfo, Map<String, Object> parsedJson) {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();

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
                    List<Map<String, Object>> exec = switch (dataType) {
                        case js, any, systemData, template ->
                                App.jdbcTemplate.exec(App.postgreSQLPoolName, Data.SELECT_SYSTEM_DATA_RANGE, arguments);
                        case userDataRSync ->
                                App.jdbcTemplate.exec(App.postgreSQLPoolName, Data.SELECT_USER_DATA_RANGE, arguments);
                        case socket ->
                                App.jdbcTemplate.exec(App.postgreSQLPoolName, Data.SELECT_SOCKET_DATA_RANGE, arguments);
                    };
                    result.put(dataType.toString(), exec);
                } else if (dbRevision < rqRevision) { //Рассинхрон версий
                    List<Map<String, Object>> needUpgradeServerType = new ArrayList<>();
                    Map<String, Object> map = new HashMap<>();
                    map.put("needUpgrade", dbRevision);
                    needUpgradeServerType.add(map);

                    result.put(dataType.toString(), needUpgradeServerType);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Блок обновления
        insertData(userSessionInfo, parsedJson, DataType.userDataRSync.name(), result); //Он может прийти пустым, так как просто человечек не залогинен
        insertData(userSessionInfo, parsedJson, DataType.socket.name(), result);

        if (result.containsKey(DataType.socket.name())) {
            updateSocketParentData(result.get(DataType.socket.name()));
        }

        String json = UtilJson.toStringPretty(result, "{}");
        System.out.println("Sync.sync() => " + json);
        return json;
    }

    private static void updateSocketParentData(List<Map<String, Object>> socketData) {
        //Сокетные данные немного кастомная история **Описание можете посмотреть в Enum
        Map<String, List<Map<String, Object>>> needParentDataMap = new HashMap<>();
        for (Map<String, Object> item : socketData) {
            String parentUuidData = (String) item.get("parent_uuid_data");
            if (parentUuidData != null && !"".equals(parentUuidData)) {
                if (!needParentDataMap.containsKey(parentUuidData)) {
                    needParentDataMap.put(parentUuidData, new ArrayList<>());
                }
                needParentDataMap.get(parentUuidData).add(item);
            }
        }
        if (needParentDataMap.size() > 0) {
            try {
                //Формируем динамический SQL c конструкцией IN
                DataByParent dataByParent = new DataByParent(needParentDataMap.keySet().stream().toList());
                //Получаем все исходные данные через связку uuid = parent_uuid
                List<Map<String, Object>> exec = App.jdbcTemplate.exec(App.postgreSQLPoolName, dataByParent, dataByParent.getSqlArguments());
                for (Map<String, Object> item : exec) {
                    //Получаем uuid_data у родителя
                    String uuidData = (String) item.get("uuid_data");
                    //Пробегаемся по всем зависимым данным пустышек и заменяем value родительским value
                    for (Map<String, Object> item2 : needParentDataMap.get(uuidData)) {
                        item2.put("value", item.get("value_data"));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void insertData(UserSessionInfo userSessionInfo, Map<String, Object> parsedJson, String key, Map<String, List<Map<String, Object>>> result) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) parsedJson.get(key);
        if (dataList != null) {
            for (Map<String, Object> dataItem : dataList) {
                try {
                    Map<String, Object> arguments = App.jdbcTemplate.createArguments();
                    arguments.putAll(dataItem);
                    userSessionInfo.appendAuthJdbcTemplateArguments(arguments);
                    List<Map<String, Object>> exec = App.jdbcTemplate.exec(App.postgreSQLPoolName, Data.INSERT, arguments);
                    if (exec.size() > 0 && exec.get(0).containsKey("new_id_revision")) {
                        String newIdRevisionString = (String) exec.get(0).get("new_id_revision");
                        if (newIdRevisionString != null && Util.isNumeric(newIdRevisionString)) {
                            dataItem.put("revision_data", Long.parseLong(newIdRevisionString));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!result.containsKey(key)) {
                result.put(key, new ArrayList<>());
            }
            mergeRevision(dataList, result.get(key));
        }
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

    public static void mergeRevision(List<Map<String, Object>> listInsertItem, List<Map<String, Object>> listResultItem) {
        for (Map<String, Object> insertItem : listInsertItem) {
            boolean find = false;
            for (Map<String, Object> item : listResultItem) {
                if (item.get("uuid").equals(insertItem.get("uuid_data"))) {
                    item.clear(); //Что бы не передавать туже самую информацию
                    item.put("uuid", insertItem.get("uuid_data"));
                    item.put("revision", insertItem.get("revision_data"));
                    find = true;
                    break;
                }
            }
            if (!find) {
                Map<String, Object> appendMap = new HashMap<>();
                appendMap.put("uuid", insertItem.get("uuid_data")); //Только номер ревизии заполняем, что бы не передавать повторно информацию
                appendMap.put("revision", insertItem.get("revision_data")); //Только номер ревизии заполняем, что бы не передавать повторно информацию
                /*for (Object key : insertItem.keySet()) {
                    String newKey = key.toString().replace("_data", "");
                    appendMap.put(newKey, insertItem.get(key));
                }*/
                listResultItem.add(appendMap);
            }
        }
        listResultItem.sort((lhs, rhs) -> {
            long l = Long.parseLong(lhs.get("revision").toString());
            long r = Long.parseLong(rhs.get("revision").toString());
            return Long.compare(l, r);
        });
    }
}
