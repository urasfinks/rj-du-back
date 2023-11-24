package ru.jamsys.mistercraft;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import ru.jamsys.*;
import ru.jamsys.component.JsonSchema;
import ru.jamsys.mistercraft.handler.http.HttpHandler;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class JRetHandler {
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
                try {
                    httpHandler.handler(jRet, userSessionInfo);
                } catch (Exception e) {
                    jRet.addException(e);
                }
            } else {
                jRet.addException(mapWrapJsonToObject.getException());
            }
        }

        return jRet;
    }

    public JsonHttpResponse getJRet(String postBody, boolean checkAuthHeader, String authHeader, String schemaValidation, HttpHandler httpHandler, boolean debug) {
        Util.logConsole("Request(" + httpHandler.toString() + "): " + postBody);
        JsonHttpResponse jRet = getJsonHttpResponse(postBody, checkAuthHeader, authHeader, schemaValidation, httpHandler);
        jRet.getData().remove("request");
        if (debug) {
            Util.logConsole("Response: " + jRet);
        }
        return jRet;
    }

    public ResponseEntity<?> getResponseEntity(String postBody, boolean checkAuthHeader, String authHeader, String schemaValidation, HttpHandler httpHandler) {
        return getResponseEntity(postBody, checkAuthHeader, authHeader, schemaValidation, httpHandler, true);
    }

    public ResponseEntity<?> getResponseEntity(String postBody, boolean checkAuthHeader, String authHeader, String schemaValidation, HttpHandler httpHandler, boolean debug) {
        return getJRet(postBody, checkAuthHeader, authHeader, schemaValidation, httpHandler, debug).getResponseEntity();
    }

    UserSessionInfo getDeviceUuid(String valueHeaderAuthorization) {
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
                            userSessionInfo.setVersion(Long.parseLong(map[0].substring(1)));
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

    Map<String, String> getPostData(HttpServletRequest request) {
        Map<String, String> result = new LinkedHashMap<>();
        List<String> list = Collections.list(request.getParameterNames());
        for (String item : list) {
            result.put(item, request.getParameter(item));
        }
        return result;
    }
}
