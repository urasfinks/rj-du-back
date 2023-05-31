package ru.jamsys.mistercraft;

public enum DataType {
    //JSON data
    template,
    systemData,
    userDataRSync,
    socket,
    //NOT JSON data
    js,
    any;

    public boolean isUserData(String dataType) {
        return isUserData(DataType.valueOf(dataType));
    }

    public boolean isUserData(DataType dataType) {
        return dataType == DataType.userDataRSync;
    }

    public boolean isUserData() {
        return isUserData(this);
    }

}
