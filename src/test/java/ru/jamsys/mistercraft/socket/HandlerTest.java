package ru.jamsys.mistercraft.socket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import ru.jamsys.App;
import ru.jamsys.message.Message;
import ru.jamsys.message.MessageImpl;

class HandlerTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.context = SpringApplication.run(App.class, args);
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

    String getUpdate() {
        return """
                {
                    "request": {
                        "operation": "UPDATE",
                        "uuid_data": "test",
                        "data":{
                            "key": "value",
                            "key2": {
                                "x": 1
                            }
                        }
                    }
                }
                """;
    }

    @Test
    void onRead() {
        Assertions.assertTrue(validate(getSubscribe()), "#1");
        Assertions.assertTrue(validate(getUnsubscribe()), "#2");
        Assertions.assertTrue(validate(getUpdate()), "#3");
    }

    private boolean validate(String data){
        RequestMessageReader handler = App.context.getBean(RequestMessageReader.class);
        RequestMessage message = new RequestMessage();
        message.setBody(data);
        return handler.onRead(message);
    }
}