package ru.jamsys;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.extension.UniqueClassNameImpl;
import ru.jamsys.core.flat.util.UtilListSort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

// Создан для связывания 6 значного кода с uuidData для удобства подключения к данным через код
// Аналог короткой ссылки
// Так же решает задачу проверки произвольного получения данных по uuidData, если тут числится - значит данные были открыты

@Component
public class ManagerCodeLink {

    private final Broker<ManagerCodeLinkItem> broker;

    private final Map<Integer, ManagerCodeLinkItem> codeMap = new HashMap<>(); // 12356: dataUuid

    private final Map<String, ManagerCodeLinkItem> uuidMap = new HashMap<>(); // dataUuid: 12356

    private final ConcurrentLinkedDeque<Integer> queueShuffleCode = new ConcurrentLinkedDeque<>();

    public ManagerCodeLink(ApplicationContext applicationContext, ManagerBroker managerBroker) {
        List<Integer> codes = new ArrayList<>();
        int start = 100000;
        int end = 999999;
        for (int i = start; i < end; i++) {
            codes.add(i);
        }
        UtilListSort.shuffle(codes);
        queueShuffleCode.addAll(codes);

        this.broker = managerBroker.initAndGet(
                UniqueClassNameImpl.getClassNameStatic(ManagerCodeLink.class, null, applicationContext),
                ManagerCodeLinkItem.class,
                this::onDrop
        );
    }

    public ManagerCodeLinkItem add(String uuidData) {
        if (queueShuffleCode.isEmpty()) {
            throw new RuntimeException("QueueShuffleCode is empty");
        }
        Integer code = queueShuffleCode.pollFirst();
        if (code == null) {
            throw new RuntimeException("QueueShuffleCode return null value");
        }
        ManagerCodeLinkItem managerCodeLinkItem = new ManagerCodeLinkItem(uuidData, code);
        codeMap.put(code, managerCodeLinkItem);
        uuidMap.put(uuidData, managerCodeLinkItem);

        broker.add(managerCodeLinkItem, 60 * 60 * 1000L); // 1 час
        return managerCodeLinkItem;
    }

    public ManagerCodeLinkItem getByCode(Integer code) {
        return codeMap.get(code);
    }

    public ManagerCodeLinkItem getByUuidData(String uuidData) {
        return uuidMap.get(uuidData);
    }

    private void onDrop(ManagerCodeLinkItem managerCodeLinkItem) {
        codeMap.remove(managerCodeLinkItem.getCode());
        uuidMap.remove(managerCodeLinkItem.getUuidData());
        queueShuffleCode.addLast(managerCodeLinkItem.getCode());
    }

}
