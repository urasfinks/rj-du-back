package ru.jamsys.mistercraft.handler.http;

import ru.jamsys.App;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.Util;
import ru.jamsys.mistercraft.DataType;
import ru.jamsys.mistercraft.UserSessionInfo;
import ru.jamsys.mistercraft.jt.Data;
import ru.jamsys.mistercraft.jt.DataByParent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sync implements HttpHandler {
    @Override
    public void handler(JsonHttpResponse jRet, UserSessionInfo userSessionInfo) {
        @SuppressWarnings("unchecked")
        Map<String, Object> parsedJson = (Map<String, Object>) jRet.getData().get("request");

        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        int totalCounterItem = 0;
        int totalByte = 0;
        //Блок выгрузки
        Map<String, Long> needUpgrade = new HashMap<>();
        try {
            // Для начала удалим, всё что уже всё на устройстве)
            //Да, это всё равно пойдёт ревизией обратно, но уже немного подрезанное (зануление value_data)
            if (parsedJson.containsKey("removed")) {
                @SuppressWarnings("unchecked")
                List<String> removed = (List<String>) parsedJson.get("removed");
                for (String uuid : removed) {
                    remove(uuid, userSessionInfo);
                }
            }
            @SuppressWarnings("unchecked")
            Map<String, Long> rqMaxRevisionByType = (Map<String, Long>) parsedJson.get("maxRevisionByType");
            Map<String, Long> dbMaxRevisionByType = getMaxRevisionByType(userSessionInfo);

            for (DataType dataType : DataType.values()) {
                long rqRevision = ((Number) rqMaxRevisionByType.getOrDefault(dataType.toString(), 0L)).longValue();
                long dbRevision = dbMaxRevisionByType.getOrDefault(dataType.toString(), 0L);
                if (dbRevision > rqRevision) {
                    Map<String, Object> arguments = App.jdbcTemplate.createArguments();
                    arguments.put("type_data", dataType.toString());
                    arguments.put("revision_data", rqRevision);
                    userSessionInfo.appendAuthJdbcTemplateArguments(arguments);
                    List<Map<String, Object>> exec = switch (dataType) {
                        case js, any, systemData, template, json, blob ->
                                App.jdbcTemplate.execute(App.postgresqlPoolName, Data.SELECT_SYSTEM_DATA_RANGE, arguments);
                        case userDataRSync, blobRSync ->
                                App.jdbcTemplate.execute(App.postgresqlPoolName, Data.SELECT_USER_DATA_RANGE, arguments);
                        case socket ->
                                App.jdbcTemplate.execute(App.postgresqlPoolName, Data.SELECT_SOCKET_DATA_RANGE, arguments);
                    };
                    totalCounterItem += exec.size();
                    for (Map<String, Object> item : exec) {
                        //Что бы канал слишком не забивать данными, немного почистим содержимое удаляемых данных
                        if (item.get("is_remove").toString().equals("1")) {
                            item.remove("value");
                            item.remove("date_update");
                            item.remove("date_add");
                            // Низя удалять parentUuid, key  так как по нему могут быть слушатели
                            // DynamicPage._subscribedOnReload из Enum SubscribeReloadGroup
                            //item.remove("parent_uuid");
                            //item.remove("key");
                            // Низя удалять revision, а то дальше где-то NPE)
                            //item.remove("revision");
                        }
                        if (item.containsKey("value") && item.get("value") != null) {
                            totalByte += ((String) item.get("value")).length();
                        }
                    }
                    result.put(dataType.toString(), exec);
                } else if (dbRevision < rqRevision) { //Рассинхрон версий
                    needUpgrade.put(dataType.toString(), dbRevision);
                }
            }
        } catch (Exception e) {
            jRet.addException(e);
            e.printStackTrace();
        }
        if (jRet.isStatus()) {
            //Блок обновления
            //userDataRSync может прийти пустым, так как просто человечек не залогинен
            insertData(userSessionInfo, parsedJson, DataType.userDataRSync.name(), result);
            //blobRSync может прийти пустым, так как просто человечек не залогинен
            insertData(userSessionInfo, parsedJson, DataType.blobRSync.name(), result);
            insertData(userSessionInfo, parsedJson, DataType.socket.name(), result);

            // Если пришли новые данные с типом socket у которых установлен parrent_uuid
            // Скорее всего это новая установка (наследование) и надо по мимо регистрации их в серверной БД
            // Надо выдать последнюю ревизию родителя

            if (result.containsKey(DataType.socket.name())) {
                updateSocketParentData(result.get(DataType.socket.name()));
            }
            jRet.addData("totalByte", totalByte);
            jRet.addData("totalCountItem", totalCounterItem);
            jRet.addData("upgrade", sizeControl(result, jRet));
            jRet.addData("serverNeedUpgrade", needUpgrade);
        }
    }

    //#1
    private static void remove(String uuid, UserSessionInfo userSessionInfo) throws Exception {
        Map<String, Object> arguments = App.jdbcTemplate.createArguments();
        userSessionInfo.appendAuthJdbcTemplateArguments(arguments);
        arguments.put("uuid_data", uuid);
        App.jdbcTemplate.execute(App.postgresqlPoolName, Data.REMOVE, arguments);
    }

    //#2
    private static Map<String, Long> getMaxRevisionByType(UserSessionInfo userSessionInfo) throws Exception {
        Map<String, Long> dbMapRevision = new HashMap<>();
        Map<String, Object> arguments = App.jdbcTemplate.createArguments();
        userSessionInfo.appendAuthJdbcTemplateArguments(arguments);
        List<Map<String, Object>> exec = App.jdbcTemplate.execute(App.postgresqlPoolName, Data.SELECT_MAX_REVISION_BY_TYPE, arguments);
        for (Map<String, Object> row : exec) {
            dbMapRevision.put((String) row.get("key"), (Long) row.get("max"));
        }
        return dbMapRevision;
    }

    //#3
    private static void insertData(UserSessionInfo userSessionInfo, Map<String, Object> parsedJson, String dataTypeName, Map<String, List<Map<String, Object>>> result) {
        // Изначально эта функция была сделана для решения следующей задачи:
        // Так как изначально происходит выборка по максимальным ревизиям на устройстве
        // а только потом идут инсерты новых данных с устройства на серверную БД
        // на устройство надо отправить
        // Когда происходят инсерты, нам на устройство надо вернуть новое установленное значение ревизии

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> listDataToInsert = (List<Map<String, Object>>) parsedJson.get(dataTypeName);
        if (listDataToInsert != null) {
            for (Map<String, Object> dataToInsert : listDataToInsert) {
                try {
                    Map<String, Object> arguments = App.jdbcTemplate.createArguments();
                    arguments.putAll(dataToInsert);
                    userSessionInfo.appendAuthJdbcTemplateArguments(arguments);
                    List<Map<String, Object>> exec = App.jdbcTemplate.execute(App.postgresqlPoolName, Data.INSERT, arguments);
                    if (exec.size() > 0 && exec.get(0).containsKey("new_id_revision")) {
                        String newIdRevisionString = (String) exec.get(0).get("new_id_revision");
                        if (newIdRevisionString != null && Util.isNumeric(newIdRevisionString)) {
                            dataToInsert.put("revision_data", Long.parseLong(newIdRevisionString));
                        } else {
                            dataToInsert.put("revision_data", null);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // Если не нашлось данных, которые надо обновлять по разности ревизий, а здесь у нас происходят инсерты
            // добавляем ключ
            if (!result.containsKey(dataTypeName)) {
                result.put(dataTypeName, new ArrayList<>());
            }
            // Мержим с зачисткой данных, что бы не передавать то, что нам отправлено,
            // оставим только uuid и новый номер назначенной ревизии
            mergeRevision(listDataToInsert, result.get(dataTypeName));
        }
    }

    //#4
    public static void mergeRevision(List<Map<String, Object>> listDataInserted, List<Map<String, Object>> listResultData) {
        // Так как у нас сначала происходит выборка данных в серверной БД
        // А после происходит обновление данных пришедших в запросе
        // Первичные отобранные данные могли измениться, поэтому мы им будем обнуляем данные
        // Что бы не посылать клиенту теже данны которые он нам прислал в запросе
        for (Map<String, Object> dataInserted : listDataInserted) {
            if (dataInserted.get("revision_data") == null) {
                // revision_data может вернуться пустой после Insert
                // если мы попытались обновить существующие сокетные данные при помощи синхронизация
                // Обновление сокетных данных надо делать через rest api
                continue;
            }

            boolean find = false;
            for (Map<String, Object> resultData : listResultData) {
                if (resultData.get("uuid").equals(dataInserted.get("uuid_data"))) {
                    resultData.clear(); //Что бы не передавать туже самую информацию
                    resultData.put("uuid", dataInserted.get("uuid_data"));
                    resultData.put("revision", dataInserted.get("revision_data"));
                    resultData.put("parent_uuid", dataInserted.get("parent_uuid_data"));
                    find = true;
                    break;
                }
            }
            //Есть вероятность, что добавляемые данные не попали в первичную выборку по разности ревизий
            // Добавляем их, что бы оповестить клиентскую часть, что этим данным назначен такой номер ревизии
            if (!find) {
                Map<String, Object> appendMap = new HashMap<>();
                appendMap.put("uuid", dataInserted.get("uuid_data")); //Только номер ревизии заполняем, что бы не передавать повторно информацию
                appendMap.put("revision", dataInserted.get("revision_data")); //Только номер ревизии заполняем, что бы не передавать повторно информацию
                appendMap.put("parent_uuid", dataInserted.get("parent_uuid_data"));
                listResultData.add(appendMap);
            }
        }
        // Результрующий список должен быть отсортирован по ревизиям
        // Ревизии имеют одну последовательность для всех типов данных
        // Если клиентская часть зависнет при update данных и данные будут не отсортированы
        // Мы можем получить пробелы не обновлённых данных на клиентской стороне
        // Так как функция getMaxRevisionByType при последующей синхронизации вернёт самую максимальную ревизию по типу
        listResultData.sort((lhs, rhs) -> {
            long l = Long.parseLong(lhs.get("revision").toString());
            long r = Long.parseLong(rhs.get("revision").toString());
            return Long.compare(l, r);
        });
    }

    //#5
    private static void updateSocketParentData(List<Map<String, Object>> listResultSocketData) {
        //Сокетные данные немного кастомная история **Описание можете посмотреть в Enum
        Map<String, List<Map<String, Object>>> needParentDataMap = new HashMap<>();
        for (Map<String, Object> resultSocketData : listResultSocketData) {
            String parentUuidData = (String) resultSocketData.get("parent_uuid");
            if (parentUuidData != null && !"".equals(parentUuidData)) {
                if (!needParentDataMap.containsKey(parentUuidData)) {
                    needParentDataMap.put(parentUuidData, new ArrayList<>());
                }
                needParentDataMap.get(parentUuidData).add(resultSocketData);
            }
        }
        if (needParentDataMap.size() > 0) {
            try {
                //Формируем динамический SQL c конструкцией IN
                DataByParent dataByParent = new DataByParent(needParentDataMap.keySet().stream().toList());
                //Получаем все исходные данные через связку uuid = parent_uuid
                List<Map<String, Object>> exec = App.jdbcTemplate.execute(App.postgresqlPoolName, dataByParent, dataByParent.getSqlArguments());
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

    //#6
    public static Map<String, List<Map<String, Object>>> sizeControl(Map<String, List<Map<String, Object>>> input, JsonHttpResponse jRet) {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        int limitByte = 100 * 1024;
        int countItem = 0;
        jRet.addData("limitByte", limitByte);
        for (String type : input.keySet()) {
            if (limitByte <= 0) {
                break;
            }
            List<Map<String, Object>> cloneObjects = new ArrayList<>();
            result.put(type, cloneObjects);
            List<Map<String, Object>> list = input.get(type);
            for (Map<String, Object> item : list) {
                if (item.containsKey("value")) {
                    int length = ((String) item.get("value")).length();
                    item.put("valueSizeByte", length);
                    limitByte -= length;
                }
                cloneObjects.add(item);
                countItem++;
                if (limitByte <= 0) {
                    break;
                }
            }
        }
        jRet.addData("limitByteOffset", limitByte);
        jRet.addData("countItem", countItem);
        return result;
    }

}
