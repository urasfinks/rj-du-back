package ru.jamsys.mistercraft.handler.http;

public enum HandlerMethod {

    SIGN_IN(new AuthSignIn()),
    SYNC(new Sync()),
    TEST(new Test()),
    SOCKET_UPDATE(new SocketUpdate()),
    GET_CODE(new AuthGetCode());

    final HttpHandler httpHandler;

    HandlerMethod(HttpHandler httpHandler) {
        this.httpHandler = httpHandler;
    }

    public HttpHandler get(){
        return httpHandler;
    }

}
