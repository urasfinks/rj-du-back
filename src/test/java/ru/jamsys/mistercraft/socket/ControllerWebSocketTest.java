package ru.jamsys.mistercraft.socket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.App;

class ControllerWebSocketTest {

    @BeforeAll
    static void beforeAll() throws Exception {
        String[] args = new String[]{};
        App.main(args);
    }

    String getSubscribe() {
        return """
                {
                    "request": {
                        "handler": "SUBSCRIBE",
                        "uuid_data": "test"
                    }
                }
                """;
    }

    String getUnsubscribe() {
        return """
                {
                    "request": {
                        "handler": "UNSUBSCRIBE",
                        "uuid_data": "test"
                    }
                }
                """;
    }

    String getBroadCast() {
        return """
                {
                    "request": {
                        "handler": "BROADCAST",
                        "uuid_data": "test",
                        "data": {
                            "message": "Hello world"
                        }
                    }
                }
                """;
    }

    @Test
    void onRead() {
        Assertions.assertTrue(validate(getSubscribe()), "#1");
        Assertions.assertTrue(validate(getUnsubscribe()), "#2");
        Assertions.assertTrue(validate(getBroadCast()), "#3");
    }

    private boolean validate(String data) {
        RequestMessageReader handler = App.context.getBean(RequestMessageReader.class);
        RequestMessage message = new RequestMessage();
        message.setBody(data);
        return handler.onRead(message);
    }

}