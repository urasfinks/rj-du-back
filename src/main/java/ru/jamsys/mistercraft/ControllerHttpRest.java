package ru.jamsys.mistercraft;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.jamsys.*;
import ru.jamsys.mistercraft.handler.http.HandlerMethod;
import ru.jamsys.mistercraft.jt.Data;
import ru.jamsys.template.Template;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@RestController
public class ControllerHttpRest extends JRetHandler {

    Configuration configuration;

    @Autowired
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

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
                .body(Util.getResourceContent(configuration.getSocketHtml(), "UTF-8"));
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

    @RequestMapping(value = "/AppComment", method = RequestMethod.POST)
    public ResponseEntity<?> appComment(@RequestBody String postBody, @RequestHeader("Authorization") String authHeader) {
        return getResponseEntity(postBody, true, authHeader, "schema/http/Comment.json", HandlerMethod.APP_COMMENT.get());
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
                    arguments.put("date_add_data", new BigDecimal(System.currentTimeMillis() / 1000));
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
        return Util.getResourceContent(configuration.getAppleAppSiteAssociation(), "UTF-8");
    }

    @GetMapping(value = "/.well-known/apple-app-site-association", produces = MediaType.APPLICATION_JSON_VALUE)
    public String addResourceHandlers2() {
        return Util.getResourceContent(configuration.getAppleAppSiteAssociation(), "UTF-8");
    }

    @GetMapping(value = "/.well-known/assetlinks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String addResourceHandlers3() {
        return Util.getResourceContent(configuration.getAssetLinks(), "UTF-8");
    }
    //<--- Mobile DeepLink

    @RequestMapping(value = "/deeplink/**", method = RequestMethod.GET)
    //public void deeplink(HttpServletResponse httpServletResponse) {
    public String deeplink() {
        return testDeeplink();
    }

    @RequestMapping(value = "/testDeeplink/**", method = RequestMethod.GET)
    public String testDeeplink() {
        HashMap<String, String> args = new HashMap<>();
        args.put("urlSchemes", configuration.getUrlSchemes());
        args.put("urlIosAppStore", configuration.getUrlIosAppStore());
        return Template.template(Util.getResourceContent(configuration.getDeeplink(), "UTF-8"), args);
    }

}
