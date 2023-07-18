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
    void socketUpdate() {
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

    @Test
    void socketInsert() {
        ControllerHttpRest controllerHttpRest = new ControllerHttpRest();
        String postData = """
                {
                    "uuid_data": "test",
                    "actions": [
                        {
                            "action": "timestamp",
                            "arguments": {
                                "field": "start"
                            }
                        }
                    ]
                }
                """;
        String authHeader = "Basic djQ6YjgxZDk4OGEtMDU1MC00Njg4LWJmNDMtNWExNTNlYzhhZWVi";

        JsonHttpResponse jsonHttpResponse = controllerHttpRest.getJsonHttpResponse(postData, true, authHeader, "schema/http/InsertSocketData.json", HandlerMethod.SOCKET_INSERT.get());
        Util.logConsole("socketTest: " + jsonHttpResponse);
        Util.sleepMillis(1000);
    }
}