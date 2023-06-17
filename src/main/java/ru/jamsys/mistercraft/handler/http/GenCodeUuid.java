package ru.jamsys.mistercraft.handler.http;

import ru.jamsys.JsonHttpResponse;
import ru.jamsys.Util;
import ru.jamsys.mistercraft.UserSessionInfo;
import ru.jamsys.mistercraft.handler.http.code.CodeObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class GenCodeUuid implements HttpHandler {

    static Map<Integer, CodeObject> map = new ConcurrentHashMap<>();
    static AtomicBoolean available = new AtomicBoolean(true);

    @Override
    public void handler(JsonHttpResponse jRet, UserSessionInfo userSessionInfo) {
        clearMap();
        sync(jsonHttpResponse -> {
            int maxCount = 5000;
            int count = 0;
            while (true) {
                Integer random = Util.random(100000, 999999);
                if (!map.containsKey(random)) {
                    CodeObject codeObject = new CodeObject();
                    codeObject.setCode(random);
                    map.put(random, codeObject);
                    jsonHttpResponse.addData("code", codeObject.getCode());
                    jsonHttpResponse.addData("uuid", codeObject.getUuid());
                    break;
                }
                if (count++ > maxCount) {
                    break;
                }
            }
        }, jRet);
    }

    private void clearMap() {
        Integer[] integers = map.keySet().toArray(new Integer[0]);
        long expiredTime = System.currentTimeMillis() - 5 * 60 * 1000;
        int countRemove = 0;
        for (Integer integer : integers) {
            CodeObject codeObject = map.get(integer);
            if (codeObject != null && expiredTime > codeObject.getTimestampAdd()) {
                map.remove(integer);
                countRemove++;
            }
        }
        if (countRemove > 0) {
            Util.logConsole("Remove expired CodeObject: " + countRemove);
        }
    }

    private void sync(Consumer<JsonHttpResponse> cons, JsonHttpResponse jRet) {
        int count = 0;
        int maxCount = 70; //7 sec
        while (true) {
            if (available.compareAndSet(true, false)) {
                try {
                    cons.accept(jRet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                available.set(true);
                break;
            }
            Util.sleepMillis(100);
            if (count++ > maxCount) {
                break;
            }
        }
    }
}
