package ru.jamsys.mistercraft.handler.http;

import ru.jamsys.App;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.mistercraft.UserSessionInfo;
import ru.jamsys.mistercraft.jt.Data;
import ru.jamsys.mistercraft.jt.Device;
import ru.jamsys.mistercraft.jt.User;

import java.util.List;
import java.util.Map;

public class AuthSignIn implements HttpHandler {
    @Override
    public void handler(JsonHttpResponse jRet, UserSessionInfo userSessionInfo) {
        //{"mail":"urasfinks@yandex.ru", "code":12345}
        @SuppressWarnings("unchecked")
        Map<String, Object> req = (Map<String, Object>) jRet.getData().get("request");
        String mail = (String) req.get("mail");
        Integer code = (Integer) req.get("code");
        boolean isAppleReviewAppStore = false;
        if (mail.equals("admin@admin.ru") && code != null && code == 214365) {//Apple Review App Store
            isAppleReviewAppStore = true;
            req.put("code", null);
        } else if (code == null) {
            jRet.addException("Код не может быть пустым");
        }

        List<Map<String, Object>> user = null;
        if (jRet.isStatus()) {
            try {
                if (isAppleReviewAppStore) {
                    user = App.jdbcTemplate.execute(App.postgresqlPoolName, User.GET_BY_CODE_APPLE_REVIEW, req);
                } else {
                    user = App.jdbcTemplate.execute(App.postgresqlPoolName, User.GET_BY_CODE, req);
                }
                if (user.size() == 0) {
                    jRet.addException("No data found");
                }
            } catch (Exception e) {
                jRet.addException(e);
            }
        }
        if (jRet.isStatus()) { //Сбросим код
            try {
                App.jdbcTemplate.execute(App.postgresqlPoolName, User.RESET_CODE, req);
            } catch (Exception e) {
                jRet.addException(e);
            }
        }
        List<Map<String, Object>> device = null;
        if (jRet.isStatus()) { //Получим запись устройства
            try {
                req.put("uuid_device", userSessionInfo.getDeviceUuid());
                device = App.jdbcTemplate.execute(App.postgresqlPoolName, Device.SELECT_BY_UUID, req);
            } catch (Exception e) {
                jRet.addException(e);
            }
        }
        if (jRet.isStatus() && device != null && user != null) { //Обновим или добавим uuid устройства для пользователя
            try {
                req.put("id_user", user.get(0).get("id_user"));
                if (device.size() == 0) {
                    App.jdbcTemplate.execute(App.postgresqlPoolName, Device.INSERT, req);
                } else {
                    App.jdbcTemplate.execute(App.postgresqlPoolName, Device.UPDATE, req);
                }
            } catch (Exception e) {
                jRet.addException(e);
            }
        }
        if (jRet.isStatus()) {
            // Если всё прошло успешно, обновим не привязанные сокетные данные к персоне, что бы не потерять данные
            // при разлогинивание, когда происходит перезапись uuid устройства
            try {
                App.jdbcTemplate.execute(App.postgresqlPoolName, Data.UPDATE_ID_USER_BEFORE_SIGN_IN, req);
            } catch (Exception e) {
                jRet.addException(e);
            }
        }
        req.remove("uuid_device"); //Персональная информация, если не удалить - то доступ к ней может получить JS, а это уже не секьюрно
    }
}
