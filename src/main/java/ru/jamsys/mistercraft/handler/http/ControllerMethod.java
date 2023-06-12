package ru.jamsys.mistercraft.handler.http;

public enum ControllerMethod {

    SIGN_IN(new AuthSignIn()),
    SYNC(new Sync()),
    TEST(new Test()),
    SOCKET_UPDATE(new SocketUpdate()),
    GET_CODE(new AuthGetCode());

    final HttpHandler httpHandler;

    ControllerMethod(HttpHandler httpHandler) {
        this.httpHandler = httpHandler;
    }

    public HttpHandler get(){
        return httpHandler;
    }

}
