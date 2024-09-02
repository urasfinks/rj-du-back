package ru.jamsys;

import lombok.Getter;

@Getter
public class CodeManagerItem {

    String uuidData;
    Integer code;

    public CodeManagerItem(String uuidData, Integer code) {
        this.uuidData = uuidData;
        this.code = code;
    }

}
