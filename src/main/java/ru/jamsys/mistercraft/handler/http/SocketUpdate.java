package ru.jamsys.mistercraft.handler.http;

import ru.jamsys.App;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.UtilJson;
import ru.jamsys.WrapJsonToObject;
import ru.jamsys.component.Broker;
import ru.jamsys.component.ThreadBalancerFactory;
import ru.jamsys.jdbc.template.Executor;
import ru.jamsys.mistercraft.ControllerWebSocket;
import ru.jamsys.mistercraft.UserSessionInfo;
import ru.jamsys.mistercraft.jt.Data;
import ru.jamsys.mistercraft.socket.RequestMessage;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SocketUpdate implements HttpHandler {

    final Broker broker;
    final ThreadBalancerFactory threadBalancerFactory;

    public SocketUpdate() {
        this.broker = App.context.getBean(Broker.class);
        threadBalancerFactory = App.context.getBean(ThreadBalancerFactory.class);
    }

    @Override
    public void handler(JsonHttpResponse jRet, UserSessionInfo userSessionInfo) {
        // req.get("uuid_data") - точно должен прийти на PRIMARY данные
        // Теоретически существует uuid_data и parent_uuid_data - почитайте DataType.java как это сопоставляется с сокетными данными

        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) jRet.getData().get("request");
        @SuppressWarnings("unchecked")
        Map<String, Object> requestData = (Map<String, Object>) request.get("data");
        Map<String, Object> merge = new LinkedHashMap<>();

        List<Map<String, Object>> listData = null;

        if (jRet.isStatus()) { //Проверка прав доступа
            Map<String, Object> arguments = App.jdbcTemplate.createArguments();
            arguments.put("uuid_data", request.get("uuid_data"));
            try {
                userSessionInfo.appendAuthJdbcTemplateArguments(arguments);
                listData = App.jdbcTemplate.execute(App.postgresqlPoolName, Data.CHECK_PERMISSION_SOCKET_DATA, arguments);
            } catch (Exception e) {
                e.printStackTrace();
                jRet.addException(e);
            }
        }

        if (jRet.isStatus() && (listData == null || listData.size() == 0)) {
            jRet.addException("Нет доступа");
        }
        Executor executor = null;
        if (jRet.isStatus()) {
            try {
                executor = App.jdbcTemplate.getExecutor(App.postgresqlPoolName);
            } catch (Exception e) {
                e.printStackTrace();
                jRet.addException(e);
            }
        }
        if (executor == null) {
            jRet.addException("Пустой executor");
        }

        if (jRet.isStatus()) { //Получаем главную запись с данными для последующего обновления
            try {
                Map<String, Object> arguments = App.jdbcTemplate.createArguments();
                arguments.put("uuid_data", request.get("uuid_data"));
                listData = executor.execute(Data.GET_PRIMARY_SOCKET_DATA, arguments);
            } catch (Exception e) {
                e.printStackTrace();
                jRet.addException(e);
            }
        }

        if (jRet.isStatus() && (listData == null || listData.size() == 0)) {
            jRet.addException("Ключевая запись не найдена");
        }

        String dbValue = null;

        if (jRet.isStatus() && listData != null) {
            dbValue = (String) listData.get(0).get("value_data");
        }

        if (jRet.isStatus() && (dbValue == null || "".equals(dbValue.trim()))) { //Проверяем, что данные в БД не пустые
            jRet.addException("Данные по сокету пусты");
        }

        WrapJsonToObject<Map<String, Object>> mapWrapJsonToObject = null;
        if (jRet.isStatus()) { //Парсим данные из БД
            mapWrapJsonToObject = UtilJson.toMap(dbValue);
        }

        if (jRet.isStatus() && mapWrapJsonToObject == null) {
            jRet.addException("Пустые сокетные данные в серверной БД");
        }

        if (jRet.isStatus() && mapWrapJsonToObject != null && mapWrapJsonToObject.getException() != null) { //Проверяем наличие ошибок при парсинге
            jRet.addException(mapWrapJsonToObject.getException());
        }

        if (jRet.isStatus() && mapWrapJsonToObject != null) { //Если всё ок - вливаем данные из БД в мапу + мержим с входными данными запроса
            merge.putAll(mapWrapJsonToObject.getObject());
            merge.putAll(requestData);
            //Если любой ключ равен null - удаляем ключ
            removeNullValue(merge);
        }

        if (jRet.isStatus() && listData != null) { //Обновляем главные сокетные данные
            try {
                Map<String, Object> arguments = App.jdbcTemplate.createArguments();
                arguments.put("value_data", UtilJson.toStringPretty(merge, "{}"));
                arguments.put("id_data", listData.get(0).get("id_data"));
                executor.execute(Data.UPDATE_PRIMARY_SOCKET_DATA, arguments);
            } catch (Exception e) {
                jRet.addException(e);
            }
        }
        if (jRet.isStatus() && listData != null) { //Обновляем ревизии дочерних сокетных данных
            try {
                Map<String, Object> arguments = App.jdbcTemplate.createArguments();
                arguments.put("uuid_data", request.get("uuid_data"));
                executor.execute(Data.UPDATE_SECONDARY_SOCKET_DATA, arguments);
            } catch (Exception e) {
                jRet.addException(e);
            }
        }
        if (jRet.isStatus()) { //Закинем на обработку данные для рассылки по сокетам
            RequestMessage requestMessage = new RequestMessage();

            Map<Object, Object> requestObject = new HashMap<>();
            requestObject.put("handler", "BROADCAST");
            requestObject.put("uuid_data", request.get("uuid_data"));
            requestObject.put("data", getBroadcastData());

            Map<Object, Object> wrapRequestObject = new HashMap<>();
            wrapRequestObject.put("request", requestObject);

            requestMessage.setBody(UtilJson.toStringPretty(wrapRequestObject, "{}"));
            broker.add(RequestMessage.class, requestMessage);
            threadBalancerFactory.getThreadBalancer(ControllerWebSocket.nameSocketRequestReader).wakeUp();
        }
        try { //Освобождаем блокировку
            executor.execute(Data.UNLOCK, App.jdbcTemplate.createArguments());
            executor.close();
        } catch (Exception e) {
            jRet.addException(e);
        }
    }

    private Map<String, Object> getBroadcastData() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("handler", "SYNC");
        result.put("response", data);
        return result;
    }

    public static void removeNullValue(Map references) {
        Object[] objects = references.keySet().toArray();
        for (Object key : objects) {
            if (references.get(key) == null) {
                references.remove(key);
            }
        }
    }

}
