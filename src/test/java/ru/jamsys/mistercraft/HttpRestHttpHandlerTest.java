package ru.jamsys.mistercraft;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.App;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.Util;
import ru.jamsys.mistercraft.handler.http.HandlerMethod;

class HttpRestHttpHandlerTest {

    @BeforeAll
    static void beforeAll() throws Exception {
        String[] args = new String[]{};
        App.main(args);
    }

    @Test
    void socketTest() {
        ControllerHttpRest controllerHttpRest = new ControllerHttpRest();
        String postData = """
                {
                    "uuid_data": "test",
                    "data":{
                        "test": "Hello world 10",
                        "test2": "Hello world 11"
                    }
                }
                """;
        String authHeader = "Basic djQ6ZmI4OGNiZjgtYTk1OS00ZWIxLWFhZmQtNzNjOTgxYzRjMmU1";

        JsonHttpResponse jsonHttpResponse = controllerHttpRest.getJsonHttpResponse(postData, true, authHeader, "schema/http/UpdateSocketData.json", HandlerMethod.SOCKET_UPDATE.get());
        Util.logConsole("socketTest: " + jsonHttpResponse);
        Util.sleepMillis(1000);
    }
}