package ru.jamsys.mistercraft.handler.http.socket_insert;

import ru.jamsys.App;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.UtilFileResource;
import ru.jamsys.UtilJson;
import ru.jamsys.component.JsonSchema;

import java.util.Map;

public enum HandlerMethod {

    TIMESTAMP(new TimestampHandlerImpl(), "schema/http/InsertSocketDataTimestamp.json");

    final Handler handler;
    final String schemaValidation;

    HandlerMethod(Handler handler, String schemaArguments) {
        this.handler = handler;
        this.schemaValidation = schemaArguments;
    }

    public Handler get() {
        return handler;
    }

    public boolean validateArguments(Map<String, Object> args, JsonHttpResponse jRet) {
        if (schemaValidation != null) {
            String postBody = UtilJson.toString(args, "{}");
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
        return jRet.isStatus();
    }

}
