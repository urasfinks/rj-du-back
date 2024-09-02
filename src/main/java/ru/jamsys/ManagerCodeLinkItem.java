package ru.jamsys;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ManagerCodeLinkItem {

    String uuidData;
    Integer code;

    public ManagerCodeLinkItem(String uuidData, Integer code) {
        this.uuidData = uuidData;
        this.code = code;
    }

}
