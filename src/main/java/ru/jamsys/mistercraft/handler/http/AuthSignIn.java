package ru.jamsys.mistercraft.handler.http;

import ru.jamsys.App;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.mistercraft.UserSessionInfo;
import ru.jamsys.mistercraft.jt.Device;
import ru.jamsys.mistercraft.jt.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthSignIn implements HttpHandler {
    @Override
    public void handler(JsonHttpResponse jRet, UserSessionInfo userSessionInfo) {
        //{"login":"urasfinks@yandex.ru", "code":12345}
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
        List<Map<String, Object>> user = null;
        if (jRet.isStatus()) {
            try {
                user = App.jdbcTemplate.exec(App.postgreSQLPoolName, User.GET_BY_CODE, req);
                if (user.size() == 0) {
                    jRet.addException("No data found");
                }
            } catch (Exception e) {
                jRet.addException(e);
            }
        }
        if (jRet.isStatus()) { //Сбросим код
            try {
                App.jdbcTemplate.exec(App.postgreSQLPoolName, User.RESET_CODE, req);
            } catch (Exception e) {
                jRet.addException(e);
            }
        }
        List<Map<String, Object>> device = null;
        if (jRet.isStatus()) { //Получим запись устройства
            try {
                req.put("uuid_device", userSessionInfo.getDeviceUuid());
                device = App.jdbcTemplate.exec(App.postgreSQLPoolName, Device.SELECT_BY_UUID, req);
            } catch (Exception e) {
                jRet.addException(e);
            }
        }
        if (jRet.isStatus() && device != null && user != null) { //Обновим или добавим устройство
            try {
                req.put("id_user", user.get(0).get("id_user"));
                if (device.size() == 0) {
                    App.jdbcTemplate.exec(App.postgreSQLPoolName, Device.INSERT, req);
                } else {
                    App.jdbcTemplate.exec(App.postgreSQLPoolName, Device.UPDATE, req);
                }
            } catch (Exception e) {
                jRet.addException(e);
            }
        }
        req.remove("uuid_device"); //Персональная информация, если не удалить - то доступ к ней может получить JS, а это уже не секьюрно
    }
}
