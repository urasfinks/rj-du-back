package ru.jamsys.mistercraft;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.jamsys.*;
import ru.jamsys.component.JsonSchema;
import ru.jamsys.mistercraft.handler.http.HandlerMethod;
import ru.jamsys.mistercraft.handler.http.HttpHandler;
import ru.jamsys.mistercraft.jt.Data;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("unused")
@RestController
public class ControllerHttpRest {

    @Value("classpath:socket.html")
    private Resource socketHtml;

    @Value("classpath:.well-known/assetlinks.json")
    private Resource assetLinks;

    @Value("classpath:.well-known/apple-app-site-association.json")
    private Resource appleAppSiteAssociation;

    @RequestMapping(value = "/GetCode", method = RequestMethod.POST)
    public ResponseEntity<?> getCode(@RequestBody String postBody) {
        return getResponseEntity(postBody, false, null, "schema/http/GetCode.json", HandlerMethod.GET_CODE.get());
    }

    @RequestMapping(value = "/LogoutWithRemove", method = RequestMethod.GET)
    public ResponseEntity<?> logoutWithRemove(@RequestHeader("Authorization") String authHeader) {
        return getResponseEntity("{}", true, authHeader, null, HandlerMethod.LOGOUT_WITH_REMOVE.get());
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

    @RequestMapping(value = "/Timeout", method = RequestMethod.GET)
    public ResponseEntity<?> timeout() {
        try {
            Thread.sleep(5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Timeout 5sec complete");
    }

    @RequestMapping(value = "/SocketUpdate", method = RequestMethod.POST)
    public ResponseEntity<?> socketUpdate(@RequestBody String postBody, @RequestHeader("Authorization") String authHeader) {
        return getResponseEntity(postBody, true, authHeader, "schema/http/UpdateSocketData.json", HandlerMethod.SOCKET_UPDATE.get());
    }

    @RequestMapping(value = "/SocketExtend", method = RequestMethod.POST)
    public ResponseEntity<?> socketExtend(@RequestBody String postBody, @RequestHeader("Authorization") String authHeader) {
        return getResponseEntity(postBody, true, authHeader, "schema/http/InsertSocketData.json", HandlerMethod.SOCKET_EXTEND.get());
    }

    @RequestMapping(value = "/Sync", method = RequestMethod.POST)
    public ResponseEntity<?> sync(@RequestBody String postBody, @RequestHeader("Authorization") String authHeader) {
        return getResponseEntity(postBody, true, authHeader, null, HandlerMethod.SYNC.get(), false);
    }

    @RequestMapping(value = "/Data", method = RequestMethod.POST)
    public ResponseEntity<?> data(@RequestBody String postBody) {
        return getResponseEntity(postBody, false, null, "schema/http/Data.json", HandlerMethod.DATA.get(), false);
    }

    @RequestMapping(value = "/Test", method = RequestMethod.GET)
    public ResponseEntity<?> test(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return getResponseEntity("{}", true, authHeader, null, HandlerMethod.TEST.get());
    }

    @RequestMapping(value = "/GenCodeUuid", method = RequestMethod.POST)
    public ResponseEntity<?> genCodeUuid(@RequestBody String postBody, @RequestHeader("Authorization") String authHeader) {
        return getResponseEntity(postBody, true, authHeader, "schema/http/GenCodeUuid.json", HandlerMethod.GEN_CODE_UUID.get());
    }

    @RequestMapping(value = "/GetCodeUuid", method = RequestMethod.POST)
    public ResponseEntity<?> getCodeUuid(@RequestBody String postBody) {
        return getResponseEntity(postBody, false, null, "schema/http/GetCodeUuid.json", HandlerMethod.GET_CODE_UUID.get());
    }

    @RequestMapping(value = "/Comment", method = RequestMethod.POST)
    public ResponseEntity<?> comment(HttpServletRequest request) {
        String postBody = UtilJson.toString(getPostData(request), "{}");
        JsonHttpResponse jRet = getJRet(postBody, false, null, "schema/http/Comment.json", HandlerMethod.COMMENT.get(), true);
        if (jRet.isStatus()) {
            jRet.setRawBody("Спасибо, сообщение отправлено");
        } else {
            jRet.setRawBody("Сообщение не отправлено, причина: " + jRet.getDescription());
        }
        return jRet.getResponseEntity();
    }

    @RequestMapping(value = "/Upload", method = RequestMethod.POST)
    public ResponseEntity<?> upload(@RequestHeader(value = "Authorization", required = false) String authHeader, @RequestParam("name") String name, @RequestParam("key") String key, @RequestParam("file") MultipartFile file) {
        JsonHttpResponse jRet = new JsonHttpResponse();
        UserSessionInfo userSessionInfo = getDeviceUuid(authHeader);
        if (!userSessionInfo.isValidRequest()
                || !userSessionInfo.isRegister()
                || userSessionInfo.getIdUser() != 5
        ) {
            jRet.setUnauthorized();
        }
        if (jRet.isStatus() && file.isEmpty()) {
            jRet.addException("Файл пустой");
        }
        if (jRet.isStatus()) {
            try {
                String base64Data = UtilBase64.base64Encode(file.getBytes(), false);
                Map<String, Object> arguments0 = App.jdbcTemplate.createArguments();
                arguments0.put("uuid_data", name);
                List<Map<String, Object>> exec = App.jdbcTemplate.execute(App.postgresqlPoolName, Data.SELECT, arguments0);
                if (exec.isEmpty()) {
                    Map<String, Object> arguments = App.jdbcTemplate.createArguments();
                    arguments.put("uuid_data", name);
                    arguments.put("value_data", base64Data);
                    arguments.put("type_data", "blob");
                    arguments.put("parent_uuid_data", null);
                    arguments.put("date_add_data", new BigDecimal(System.currentTimeMillis()));
                    arguments.put("is_remove_data", 0);
                    arguments.put("id_user", 1);
                    arguments.put("key_data", key);
                    arguments.put("uuid_device", null);
                    arguments.put("new_id_revision", new BigDecimal(0));
                    App.jdbcTemplate.execute(App.postgresqlPoolName, Data.INSERT, arguments);
                }
            } catch (Exception e) {
                jRet.addException(e);
            }
        }
        return jRet.getResponseEntity();
    }

    //---> Mobile DeepLink
    @GetMapping(value = "/apple-app-site-association", produces = MediaType.APPLICATION_JSON_VALUE)
    public String addResourceHandlers() {
        return Util.getResourceContent(appleAppSiteAssociation, "UTF-8");
    }

    @GetMapping(value = "/.well-known/apple-app-site-association", produces = MediaType.APPLICATION_JSON_VALUE)
    public String addResourceHandlers2() {
        return Util.getResourceContent(appleAppSiteAssociation, "UTF-8");
    }

    @GetMapping(value = "/.well-known/assetlinks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String addResourceHandlers3() {
        return Util.getResourceContent(assetLinks, "UTF-8");
    }
    //<--- Mobile DeepLink

    @RequestMapping(value = "/deeplink/**", method = RequestMethod.GET)
    public void method(HttpServletResponse httpServletResponse) {
        httpServletResponse.setHeader("Location", "/");
        httpServletResponse.setStatus(302);
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
        Util.logConsole("Request: " + postBody);
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
