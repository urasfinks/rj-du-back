package ru.jamsys.web.http.sub.handler;

import ru.jamsys.core.flat.util.JsonSchema;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;

import java.util.Map;

public enum SubHandlerMethod {

    TIMESTAMP(new TimestampSubHandlerImpl(), "schema/http/InsertSocketDataTimestamp.json");

    final SubHandler handler;
    final String schemaValidation;

    SubHandlerMethod(SubHandler handler, String schemaArguments) {
        this.handler = handler;
        this.schemaValidation = schemaArguments;
    }

    public SubHandler get() {
        return handler;
    }

    public void validateOrThrow(Map<String, Object> args) throws Throwable {
        JsonSchema.validate(
                UtilJson.toString(args),
                UtilFileResource.getAsString(schemaValidation),
                name() + ": " + schemaValidation
        );
    }

}
