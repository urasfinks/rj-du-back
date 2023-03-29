package ru.jamsys.mistercraft;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.jamsys.Util;
import ru.jamsys.WrapJsonToObject;

import java.util.*;

@RestController
public class MainController {

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
    public ResponseEntity<?> home(@RequestBody String postBody, @RequestHeader("Authorization") String authorization) {
        UserSessionInfo userSessionInfo = Auth.getDeviceUuid(authorization);
        if (!userSessionInfo.isValidRequest()) {
            return auth();
        }
        WrapJsonToObject<Map> mapWrapJsonToObject = Util.jsonToObject(postBody, Map.class);
        if (mapWrapJsonToObject.getException() == null) {
            System.out.println("[" + new Date().toString() + "] Request(/sync); UserSessionInfo => isRegister: " + userSessionInfo.isRegister() + "; postBody: " + postBody);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Sync.handler(userSessionInfo, mapWrapJsonToObject.getObject()));
        } else {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(mapWrapJsonToObject.getException().toString());
        }
    }

    public ResponseEntity auth() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", "Basic realm=\"JamSys\"")
                .body("<html><body><h1>401. Unauthorized</h1></body>");

    }

}
