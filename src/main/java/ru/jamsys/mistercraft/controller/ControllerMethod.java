package ru.jamsys.mistercraft.controller;

public enum ControllerMethod {

    SIGN_IN(new AuthSignIn()),
    SYNC(new Sync()),
    TEST(new Test()),
    GET_CODE(new AuthGetCode());

    final Controller controller;

    ControllerMethod(Controller controller) {
        this.controller = controller;
    }

    public Controller get(){
        return controller;
    }

}
