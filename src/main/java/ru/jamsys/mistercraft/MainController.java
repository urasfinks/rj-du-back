package ru.jamsys.mistercraft;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.jamsys.*;
import ru.jamsys.component.JsonSchema;
import ru.jamsys.mistercraft.jt.Device;
import ru.jamsys.mistercraft.jt.User;

import java.util.*;

@RestController
public class MainController {

    JsonSchema jsonSchema;
    EMail eMail;

    @Autowired
    public void setJsonSchema(JsonSchema jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    @Autowired
    public void setEMail(EMail eMail) {
        this.eMail = eMail;
    }

    @RequestMapping(value = "/getcode", method = RequestMethod.POST)
    public ResponseEntity<?> getCode(@RequestBody String postBody) {
        System.out.println(postBody);
        //{"login":"urasfinks@yandex.ru"}
        JsonHttpResponse jRet = new JsonHttpResponse();
        if (jRet.isStatus()) {
            validate("schema/getcode.json", postBody, jRet);
        }
        if (jRet.isStatus()) {
            parseBody(postBody, jRet);
        }
        Map req = new HashMap();
        if (jRet.isStatus()) {
            try {
                req = (Map) jRet.getData().get("request");
            } catch (Exception e) {
                jRet.setException(e.toString());
            }
        }
        Map<String, Object> arguments = null;
        if (jRet.isStatus()) {
            try {
                arguments = App.jdbcTemplate.createArguments();
                arguments.put("mail", req.get("mail"));
                arguments.put("code", Util.random(10000, 99999));
                List<Map<String, Object>> user = App.jdbcTemplate.exec(App.postgreSQLPoolName, User.GET_BY_MAIL, req);
                if (user.size() == 0) {
                    App.jdbcTemplate.exec(App.postgreSQLPoolName, User.INSERT, arguments);
                } else {
                    App.jdbcTemplate.exec(App.postgreSQLPoolName, User.SET_CODE, arguments);
                }
            } catch (Exception e) {
                jRet.setException(e.toString());
            }
        }
        if (jRet.isStatus()) {
            try {
                eMail.sendCode((String) arguments.get("mail"), arguments.get("code").toString());
            } catch (Exception e) {
                jRet.setException(e.toString());
            }
        }
        return ResponseEntity
                .status(jRet.isStatus() ? HttpStatus.OK : HttpStatus.EXPECTATION_FAILED)
                .body(jRet.toString());
    }

    @RequestMapping(value = "/signin", method = RequestMethod.POST)
    public ResponseEntity<?> signIn(@RequestBody String postBody, @RequestHeader("Authorization") String authorization) {
        System.out.println(postBody);
        UserSessionInfo userSessionInfo = Auth.getDeviceUuid(authorization);
        if (!userSessionInfo.isValidRequest()) {
            return auth();
        }
        //{"login":"urasfinks@yandex.ru", "code":12345}
        JsonHttpResponse jRet = new JsonHttpResponse();
        if (jRet.isStatus()) {
            validate("schema/signin.json", postBody, jRet);
        }
        if (jRet.isStatus()) {
            parseBody(postBody, jRet);
        }
        Map req = new HashMap();
        if (jRet.isStatus()) {
            try {
                req = (Map) jRet.getData().get("request");
            } catch (Exception e) {
                jRet.setException(e.toString());
            }
        }
        List<Map<String, Object>> user = null;
        if (jRet.isStatus()) {
            try {
                user = App.jdbcTemplate.exec(App.postgreSQLPoolName, User.GET_BY_CODE, req);
                if (user.size() == 0) {
                    jRet.setException("No data found");
                }
            } catch (Exception e) {
                jRet.setException(e.toString());
            }
        }
        if (jRet.isStatus()) { //Сбросим код
            try {
                App.jdbcTemplate.exec(App.postgreSQLPoolName, User.RESET_CODE, req);
            } catch (Exception e) {
                jRet.setException(e.toString());
            }
        }
        List device = null;
        if (jRet.isStatus()) { //Получим запись устройства
            try {
                req.put("uuid_device", userSessionInfo.getDeviceUuid());
                device = App.jdbcTemplate.exec(App.postgreSQLPoolName, Device.SELECT_BY_UUID, req);
            } catch (Exception e) {
                jRet.setException(e.toString());
            }
        }
        if (jRet.isStatus()) { //Обновим или добавим устройство
            try {
                req.put("id_user", user.get(0).get("id_user"));
                if (device.size() == 0) {
                    App.jdbcTemplate.exec(App.postgreSQLPoolName, Device.INSERT, req);
                } else {
                    App.jdbcTemplate.exec(App.postgreSQLPoolName, Device.UPDATE, req);
                }
            } catch (Exception e) {
                jRet.setException(e.toString());
            }
        }
        req.remove("uuid_device"); //Персональная информация, если не удалить - то доступ к ней может получить JS, а это уже не секьюрно
        return ResponseEntity
                .status(jRet.isStatus() ? HttpStatus.OK : HttpStatus.EXPECTATION_FAILED)
                .body(jRet.toString());
    }

    private void parseBody(String postBody, JsonHttpResponse jRet) {
        WrapJsonToObject<Map> mapWrapJsonToObject = Util.jsonToObject(postBody, Map.class);
        if (mapWrapJsonToObject.getException() != null) {
            jRet.setException(mapWrapJsonToObject.getException().toString());
        } else {
            jRet.addData("request", mapWrapJsonToObject.getObject());
        }

    }

    private void validate(String schema, String postBody, JsonHttpResponse jRet) {
        try {
            JsonSchema.Result validate = jsonSchema.validate(postBody, UtilFileResource.getAsString(schema));
            if (!validate.isValidate()) {
                jRet.setException(validate.getError());
            }
        } catch (Exception e) {
            jRet.setException(e.getMessage());
        }
    }

    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    public ResponseEntity<?> hello(@RequestHeader(value = "Authorization", required = false) String authorization) {
        UserSessionInfo userSessionInfo = Auth.getDeviceUuid(authorization);
        if (!userSessionInfo.isValidRequest()) {
            return auth();
        }
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Hello world");
    }

    @RequestMapping(value = "/sync", method = RequestMethod.POST)
    public ResponseEntity<?> sync(@RequestBody String postBody, @RequestHeader("Authorization") String authorization) {
        UserSessionInfo userSessionInfo = Auth.getDeviceUuid(authorization);
        if (!userSessionInfo.isValidRequest()) {
            return auth();
        }
        WrapJsonToObject<Map> mapWrapJsonToObject = Util.jsonToObject(postBody, Map.class);
        if (mapWrapJsonToObject.getException() == null) {
            System.out.println("[" + new Date() + "] Request(/sync); UserSessionInfo => isRegister: " + userSessionInfo.isRegister() + "; postBody: " + postBody);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Sync.handler(userSessionInfo, mapWrapJsonToObject.getObject()));
        } else {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(mapWrapJsonToObject.getException().toString());
        }
    }

    private ResponseEntity<?> auth() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", "Basic realm=\"JamSys\"")
                .body("<html><body><h1>401. Unauthorized</h1></body>");

    }


}
