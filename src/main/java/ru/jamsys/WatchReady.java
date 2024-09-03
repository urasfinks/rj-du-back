package ru.jamsys;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.jamsys.core.App;

@SpringBootApplication
public class WatchReady {

    public static void main(String[] args) {
        App.springSource = WatchReady.class;
        App.getRunBuilder()
                .add("spring.config.location", "classpath:application.properties,classpath:/properties/watch-ready/application.properties")
                .runCore();

    }

}
