package ru.jamsys;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.extension.UniqueClassNameImpl;
import ru.jamsys.core.flat.util.ListSort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

// Создан для связывания 6 значного кода с uuidData для удобства подключения к данным через код
// Аналог короткой ссылки
// Так же решает задачу проверки произвольного получения данных по uuidData, если тут числится - значит данные были открыты

@Component
public class CodeManager {

    private final Broker<CodeManagerItem> broker;

    private final Map<Integer, CodeManagerItem> codeMap = new HashMap<>(); // 12356: dataUuid

    private final Map<String, CodeManagerItem> uuidMap = new HashMap<>(); // dataUuid: 12356

    private final ConcurrentLinkedDeque<Integer> queueShuffleCode = new ConcurrentLinkedDeque<>();

    public CodeManager(ApplicationContext applicationContext, ManagerBroker managerBroker) {
        List<Integer> codes = new ArrayList<>();
        int start = 100000;
        int end = 999999;
        for (int i = start; i < end; i++) {
            codes.add(i);
        }
        ListSort.shuffle(codes);
        queueShuffleCode.addAll(codes);

        this.broker = managerBroker.initAndGet(
                UniqueClassNameImpl.getClassNameStatic(CodeManager.class, null, applicationContext),
                CodeManagerItem.class,
                this::onDrop
        );
    }

    public CodeManagerItem add(String uuidData) {
        if (queueShuffleCode.isEmpty()) {
            throw new RuntimeException("QueueShuffleCode is empty");
        }
        Integer code = queueShuffleCode.pollFirst();
        if (code == null) {
            throw new RuntimeException("QueueShuffleCode return null value");
        }
        CodeManagerItem codeManagerItem = new CodeManagerItem(uuidData, code);
        codeMap.put(code, codeManagerItem);
        uuidMap.put(uuidData, codeManagerItem);

        broker.add(codeManagerItem, 60 * 60 * 1000L); // 1 час
        return codeManagerItem;
    }

    public CodeManagerItem getByCode(Integer code) {
        return codeMap.get(code);
    }

    public CodeManagerItem getByUuidData(String uuidData) {
        return uuidMap.get(uuidData);
    }

    private void onDrop(CodeManagerItem codeManagerItem) {
        codeMap.remove(codeManagerItem.getCode());
        uuidMap.remove(codeManagerItem.getUuidData());
        queueShuffleCode.addLast(codeManagerItem.getCode());
    }

}
