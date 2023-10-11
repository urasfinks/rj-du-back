package ru.jamsys.mistercraft.handler.http;

public enum HandlerMethod {

    SIGN_IN(new AuthSignIn()),
    SYNC(new Sync()),
    DATA(new Data()),
    TEST(new Test()),
    SOCKET_UPDATE(new SocketUpdate()),
    SOCKET_EXTEND(new SocketExtend()),
    GEN_CODE_UUID(new GenCodeUuid()),
    GET_CODE_UUID(new GetCodeUuid()),
    COMMENT(new Comment()),
    GET_CODE(new AuthGetCode());

    final HttpHandler httpHandler;

    HandlerMethod(HttpHandler httpHandler) {
        this.httpHandler = httpHandler;
    }

    public HttpHandler get(){
        return httpHandler;
    }

}
