package ru.jamsys;

import ru.jamsys.core.App;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.jt.Device;

import java.util.List;
import java.util.Map;

public class PromiseExtension {

    public static void thenSelectUuidDevice(Promise promiseSource) {
        promiseSource.then("SelectUuidDevice", (_, promise) -> {
            ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
            servletHandler.getRequestReader().basicAuthHandler((user, password) -> {
                if (!user.startsWith("v")) {
                    throw new RuntimeException("basic version is not defined");
                }
                promise.setRepositoryMap("uuid_device", password);
            });
        });
    }

    public static void addResponseObject(Promise promiseSource) {
        ResponseObject responseObject = new ResponseObject();
        responseObject.append("status", false);
        promiseSource.setRepositoryMapClass(ResponseObject.class, responseObject);
    }

    public static void thenSelectIdUser(Promise promiseSource) {
        promiseSource
                .extension(PromiseExtension::thenSelectUuidDevice)
                .thenWithResource("SelectIdUser", JdbcResource.class, "default", (_, promise, jdbcResource) -> {
                    List<Map<String, Object>> exec = jdbcResource.execute(
                            new JdbcRequest(Device.SELECT_BY_UUID)
                                    .addArg("id_user", promise.getRepositoryMap("id_user", Integer.class))
                                    .addArg("uuid_device", promise.getRepositoryMap("uuid_device", String.class))
                    );
                    if (exec.isEmpty()) {
                        throw new RuntimeException("Device undefined");
                    }
                    promise.setRepositoryMap("id_user", exec.getFirst().get("id_user"));
                });
    }

    public static void addTerminal(Promise promiseSource) {
        promiseSource.onComplete((_, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    ResponseObject repositoryMapClass = promise.getRepositoryMapClass(ResponseObject.class);
                    Map<String, Object> output = repositoryMapClass == null ? new HashMapBuilder<>() : repositoryMapClass;
                    output.put("status", true);
                    servletHandler.setResponseBodyFromMap(output);
                    servletHandler.responseComplete();
                })
                .onError((_, promise) -> {
                    App.error(promise.getException());
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    servletHandler.setResponseError(promise.getException().getMessage());
                    servletHandler.responseComplete();
                });
    }

}
