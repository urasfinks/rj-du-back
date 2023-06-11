package ru.jamsys.mistercraft.socket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.App;

class HandlerTest {

    @BeforeAll
    static void beforeAll() throws Exception {
        String[] args = new String[]{};
        App.main(args);
    }

    String getSubscribe() {
        return """
                {
                    "request": {
                        "operation": "SUBSCRIBE",
                        "uuid_data": "test"
                    }
                }
                """;
    }

    String getUnsubscribe() {
        return """
                {
                    "request": {
                        "operation": "UNSUBSCRIBE",
                        "uuid_data": "test"
                    }
                }
                """;
    }

    @Test
    void onRead() {
        Assertions.assertTrue(validate(getSubscribe()), "#1");
        Assertions.assertTrue(validate(getUnsubscribe()), "#2");
    }

    private boolean validate(String data) {
        RequestMessageReader handler = App.context.getBean(RequestMessageReader.class);
        RequestMessage message = new RequestMessage();
        message.setBody(data);
        return handler.onRead(message);
    }

}