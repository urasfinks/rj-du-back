package ru.jamsys;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.jamsys.core.App;

@SpringBootApplication
public class RjDuBack {

    public static void main(String[] args) {
        App.springSource = RjDuBack.class;
        App.main(args);
    }

}
