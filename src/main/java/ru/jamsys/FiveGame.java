package ru.jamsys;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.jamsys.core.App;

@SpringBootApplication
public class FiveGame {

    public static void main(String[] args) {
        App.springSource = FiveGame.class;
        App.getRunBuilder()
                .add("spring.config.location", "classpath:application.properties,classpath:/properties/5gm/application.properties")
                .runCore();

    }

}
