package ru.jamsys.mistercraft.jt;

import org.junit.jupiter.api.Test;
import ru.jamsys.Util;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataByParentTest {

    @Test
    void getSqlArguments() {
        List<String> inArgs = new ArrayList<>();
        inArgs.add("test1");
        inArgs.add("test2");

        DataByParent dataByParent = new DataByParent(inArgs);
        Util.logConsole(dataByParent.getSql());
        Util.logConsole(dataByParent.getSqlArguments().toString());
    }
}