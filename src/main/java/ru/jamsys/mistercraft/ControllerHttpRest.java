package ru.jamsys.mistercraft;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.jamsys.*;
import ru.jamsys.component.JsonSchema;
import ru.jamsys.mistercraft.handler.http.HttpHandler;
import ru.jamsys.mistercraft.handler.http.HandlerMethod;

import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("unused")
@RestController
public class ControllerHttpRest {

    @Value("classpath:socket.html")
    private Resource socketHtml;

    @RequestMapping(value = "/GetCode", method = RequestMethod.POST)
    public ResponseEntity<?> getCode(@RequestBody String postBody) {
        return getResponseEntity(postBody, false, null, "schema/http/GetCode.json", HandlerMethod.GET_CODE.get());
    }

    @RequestMapping(value = "/SignIn", method = RequestMethod.POST)
    public ResponseEntity<?> signIn(@RequestBody String postBody, @RequestHeader("Authorization") String authHeader) {
        return getResponseEntity(postBody, true, authHeader, "schema/http/SignIn.json", HandlerMethod.SIGN_IN.get());
    }

    @RequestMapping(value = "/SocketTest", method = RequestMethod.GET)
    public ResponseEntity<?> socketTest() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Util.getResourceContent(socketHtml, "UTF-8"));
    }

    @RequestMapping(value = "/SocketUpdate", method = RequestMethod.POST)
    public ResponseEntity<?> socketUpdate(@RequestBody String postBody, @RequestHeader("Authorization") String authHeader) {
        return getResponseEntity(postBody, true, authHeader, "schema/http/UpdateSocketData.json", HandlerMethod.SOCKET_UPDATE.get());
    }

    @RequestMapping(value = "/SocketInsert", method = RequestMethod.POST)
    public ResponseEntity<?> socketInsert(@RequestBody String postBody, @RequestHeader("Authorization") String authHeader) {
        return getResponseEntity(postBody, true, authHeader, "schema/http/InsertSocketData.json", HandlerMethod.SOCKET_INSERT.get());
    }

    @RequestMapping(value = "/Sync", method = RequestMethod.POST)
    public ResponseEntity<?> sync(@RequestBody String postBody, @RequestHeader("Authorization") String authHeader) {
        return getResponseEntity(postBody, true, authHeader, null, HandlerMethod.SYNC.get());
    }

    @RequestMapping(value = "/Test", method = RequestMethod.GET)
    public ResponseEntity<?> test(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return getResponseEntity("{}", true, authHeader, null, HandlerMethod.TEST.get());
    }

    @RequestMapping(value = "/GenCodeUuid", method = RequestMethod.GET)
    public ResponseEntity<?> genCodeUuid() {
        return getResponseEntity("{}", false, null, null, HandlerMethod.GEN_CODE_UUID.get());
    }

    @RequestMapping(value = "/GetCodeUuid", method = RequestMethod.POST)
    public ResponseEntity<?> getCodeUuid(@RequestBody String postBody) {
        return getResponseEntity(postBody, false, null, "schema/http/GetCodeUuid.json", HandlerMethod.GET_CODE_UUID.get());
    }

    public JsonHttpResponse getJsonHttpResponse(String postBody, boolean checkAuthHeader, String authHeader, String schemaValidation, HttpHandler httpHandler) {
        JsonHttpResponse jRet = new JsonHttpResponse();
        UserSessionInfo userSessionInfo = null;
        if (checkAuthHeader) {
            userSessionInfo = getDeviceUuid(authHeader);
            if (!userSessionInfo.isValidRequest()) {
                jRet.setUnauthorized();
            }
        }
        if (jRet.isStatus()) {
            if (schemaValidation != null) {
                try {
                    String schema = UtilFileResource.getAsString(schemaValidation);
                    JsonSchema.Result validate = App.jsonSchema.validate(postBody, schema);
                    if (!validate.isValidate()) {
                        jRet.addException("Request: " + postBody + "\n Schema: " + schema);
                        jRet.addException(validate.getError());
                    }
                } catch (Exception e) {
                    jRet.addException(e);
                }
            }
        }

        if (jRet.isStatus()) {
            WrapJsonToObject<Map<String, Object>> mapWrapJsonToObject = UtilJson.toMap(postBody);
            if (mapWrapJsonToObject.getException() == null) {
                jRet.addData("request", mapWrapJsonToObject.getObject());
                httpHandler.handler(jRet, userSessionInfo);
            } else {
                jRet.addException(mapWrapJsonToObject.getException());
            }
        }

        return jRet;
    }

    public ResponseEntity<?> getResponseEntity(String postBody, boolean checkAuthHeader, String authHeader, String schemaValidation, HttpHandler httpHandler) {
        Util.logConsole("Request: " + postBody);
        JsonHttpResponse jRet = getJsonHttpResponse(postBody, checkAuthHeader, authHeader, schemaValidation, httpHandler);
        Util.logConsole("Response: " + jRet.toString());
        jRet.getData().remove("request");
        return jRet.getResponseEntity();
    }

    private UserSessionInfo getDeviceUuid(String valueHeaderAuthorization) {
        UserSessionInfo userSessionInfo = new UserSessionInfo();
        if (valueHeaderAuthorization != null && !"".equals(valueHeaderAuthorization) && valueHeaderAuthorization.startsWith("Basic ")) {
            String[] valueExplode = valueHeaderAuthorization.split("Basic ");
            if (valueExplode.length == 2) {
                byte[] decoded = Base64.getDecoder().decode(valueExplode[1]);
                String decodedStr = new String(decoded, StandardCharsets.UTF_8);
                if (decodedStr.startsWith("v")) {
                    String[] map = decodedStr.split(":");
                    if (map.length == 2) {
                        try {
                            userSessionInfo.setVersion(Integer.parseInt(map[0].substring(1)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        userSessionInfo.setDeviceUuid(map[1]);
                    }
                }
            }
        }
        userSessionInfo.check();
        return userSessionInfo;
    }

}
