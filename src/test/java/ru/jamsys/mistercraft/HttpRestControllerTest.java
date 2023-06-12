package ru.jamsys.mistercraft;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.App;
import ru.jamsys.JsonHttpResponse;
import ru.jamsys.mistercraft.controller.ControllerMethod;

class HttpRestControllerTest {

    @BeforeAll
    static void beforeAll() throws Exception {
        String[] args = new String[]{};
        App.main(args);
    }

    @Test
    void socketTest() {
        HttpRestController httpRestController = new HttpRestController();
        String postData = """
                {
                    "uuid_data": "",
                    "data":{
                        "test": "Hello world"
                    }
                }
                """;
        String authHeader = "Basic djQ6ZmI4OGNiZjgtYTk1OS00ZWIxLWFhZmQtNzNjOTgxYzRjMmU1";

        JsonHttpResponse jsonHttpResponse = httpRestController.getJsonHttpResponse(postData, true, authHeader, "schema/SocketRestUpdate.json", ControllerMethod.SOCKET_UPDATE.get());
        System.out.println(jsonHttpResponse);
    }
}