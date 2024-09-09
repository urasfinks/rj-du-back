package ru.jamsys.promise;

import ru.jamsys.core.App;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.AuthException;
import ru.jamsys.core.extension.exception.JsonSchemaException;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.jt.Device;
import ru.jamsys.promise.repository.AuthRepository;
import ru.jamsys.promise.repository.ParsedJsonRepository;
import ru.jamsys.promise.repository.ResponseRepository;

import java.util.List;
import java.util.Map;

public class PromiseExtension {

    public static void addResponseRepository(Promise promiseSource) {
        ResponseRepository responseRepository = new ResponseRepository();
        responseRepository.append("status", false);
        promiseSource.setRepositoryMapClass(ResponseRepository.class, responseRepository);
    }

    public static void addParsedJsonRepository(Promise promiseSource) {
        promiseSource.setRepositoryMapClass(ParsedJsonRepository.class, new ParsedJsonRepository());
    }

    public static void addAuthRepository(Promise promiseSource) {
        promiseSource.setRepositoryMapClass(AuthRepository.class, new AuthRepository());
    }

    public static void thenSelectUuidDeviceRequire(Promise promiseSource) {
        promiseSource
                .extension(PromiseExtension::addAuthRepository)
                .then("SelectUuidDevice", (_, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    servletHandler.getRequestReader().basicAuthHandler((user, password) -> {
                        if (!user.startsWith("v")) {
                            throw new AuthException("basic version is not defined");
                        }
                        promise.getRepositoryMapClass(AuthRepository.class).setUuidDevice(password);
                    });
                });
    }

    // Обязательная проверка что idUser заполнен
    public static void thenSelectIdUserRequire(Promise promiseSource) {
        promiseSource
                .extension(PromiseExtension::thenSelectIdUserIfExist)
                .then("CheckIdUser", (_, promise) -> {
                    AuthRepository authRepository = promise.getRepositoryMapClass(AuthRepository.class);
                    if (authRepository.getIdUser() == null) {
                        throw new AuthException("User device undefined");
                    }
                });
    }

    // заполнение idUser без вызова исключения, если человек не авторизовался
    public static void thenSelectIdUserIfExist(Promise promiseSource) {
        promiseSource
                .extension(PromiseExtension::thenSelectUuidDeviceRequire)
                .thenWithResource("SelectIdUser", JdbcResource.class, "default", (_, promise, jdbcResource) -> {
                    AuthRepository authRepository = promise.getRepositoryMapClass(AuthRepository.class);
                    List<Map<String, Object>> exec = jdbcResource.execute(
                            new JdbcRequest(Device.SELECT_BY_UUID).addArg(authRepository.get())
                    );
                    if (!exec.isEmpty()) {
                        authRepository.setIdUser((Long) exec.getFirst().get("id_user"));
                    }
                });
    }

    public static void addTerminal(Promise promiseSource) {
        promiseSource
                .extension(PromiseExtension::addCompleteHandler)
                .extension(PromiseExtension::addErrorHandler);
    }

    public static void addCompleteHandler(Promise promiseSource) {
        promiseSource
                .onComplete((_, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    ResponseRepository repositoryMapClass = promise.getRepositoryMapClass(ResponseRepository.class);
                    Map<String, Object> output = repositoryMapClass == null ? new HashMapBuilder<>() : repositoryMapClass;
                    output.put("status", true);
                    servletHandler.setResponseBodyFromMap(output);
                    servletHandler.responseComplete();
                });
    }

    public static void addErrorHandler(Promise promiseSource) {
        promiseSource.onError((_, promise) -> errorHandler(promise));
    }

    public static void errorHandler(Promise promise) {
        App.error(promise.getException());
        ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
        servletHandler.setResponseContentType("application/json");
        Throwable exception = promise.getException();
        if (exception instanceof JsonSchemaException) {
            servletHandler.setResponseError(((JsonSchemaException) exception).getResponseError());
        } else if (exception instanceof AuthException) {
            servletHandler.setResponseUnauthorized();
            servletHandler.getCompletableFuture().complete(null);
            return;
        } else {
            servletHandler.setResponseError(promise.getException().getMessage());
        }
        servletHandler.responseComplete();
    }

}
