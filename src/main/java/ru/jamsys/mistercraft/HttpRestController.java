package ru.jamsys.mistercraft;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.jamsys.*;
import ru.jamsys.component.JsonSchema;
import ru.jamsys.mistercraft.controller.Controller;
import ru.jamsys.mistercraft.controller.ControllerMethod;

import java.util.*;

@SuppressWarnings("unused")
@RestController
public class HttpRestController {

    private static JsonSchema jsonSchema;

    @Value("classpath:socket.html")
    private Resource socketHtml;

    @Autowired
    public static void setJsonSchema(JsonSchema jsonSchema) {
        HttpRestController.jsonSchema = jsonSchema;
    }

    @RequestMapping(value = "/GetCode", method = RequestMethod.POST)
    public ResponseEntity<?> getCode(@RequestBody String postBody) {
        return handler(postBody, false, null, "schema/GetCode.json", ControllerMethod.GET_CODE.get());
    }

    @RequestMapping(value = "/SignIn", method = RequestMethod.POST)
    public ResponseEntity<?> signIn(@RequestBody String postBody, @RequestHeader("Authorization") String authHeader) {
        return handler(postBody, true, authHeader, "schema/SignIn.json", ControllerMethod.SIGN_IN.get());
    }

    @RequestMapping(value = "/SocketTest", method = RequestMethod.GET)
    public ResponseEntity<?> socket() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Util.getResourceContent(socketHtml, "UTF-8"));
    }

    @RequestMapping(value = "/SocketUpdate", method = RequestMethod.POST)
    public ResponseEntity<?> socket(@RequestBody String postBody, @RequestHeader("Authorization") String authHeader) {
        return handler(postBody, true, authHeader, "schema/SocketRestUpdate.json", ControllerMethod.SIGN_IN.get());
    }

    @RequestMapping(value = "/Sync", method = RequestMethod.POST)
    public ResponseEntity<?> sync(@RequestBody String postBody, @RequestHeader("Authorization") String authHeader) {
        return handler(postBody, true, authHeader, null, ControllerMethod.SYNC.get());
    }

    @RequestMapping(value = "/Test", method = RequestMethod.GET)
    public ResponseEntity<?> test(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return handler("{}", true, authHeader, null, ControllerMethod.TEST.get());
    }

    public ResponseEntity<?> handler(String postBody, boolean checkAuth, String authHeader, String schemaValidation, Controller controller) {
        Util.logConsole("Request: " + postBody);
        UserSessionInfo userSessionInfo = null;
        if (checkAuth) {
            userSessionInfo = Auth.getDeviceUuid(authHeader);
            if (!userSessionInfo.isValidRequest()) {
                return getUnauthorizedResponse();
            }
        }
        JsonHttpResponse jRet = new JsonHttpResponse();

        if (schemaValidation != null) {
            try {
                JsonSchema.Result validate = jsonSchema.validate(postBody, UtilFileResource.getAsString(schemaValidation));
                if (!validate.isValidate()) {
                    jRet.setException(validate.getError());
                }
            } catch (Exception e) {
                jRet.setException(e.getMessage());
            }
        }

        if (jRet.isStatus()) {
            WrapJsonToObject<Map<String, Object>> mapWrapJsonToObject = UtilJson.toMap(postBody);
            if (mapWrapJsonToObject.getException() == null) {
                jRet.addData("request", mapWrapJsonToObject.getObject());
                controller.handler(jRet, userSessionInfo);
            } else {
                jRet.setException(mapWrapJsonToObject.getException().toString());
            }
        }
        jRet.getData().remove("request");
        Util.logConsole("Response: " + jRet);
        return ResponseEntity
                .status(jRet.isStatus() ? HttpStatus.OK : HttpStatus.EXPECTATION_FAILED)
                .body(jRet.toString());
    }

    private ResponseEntity<?> getUnauthorizedResponse() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", "Basic realm=\"JamSys\"")
                .body("<html><body><h1>401. Unauthorized</h1></body>");
    }

}
