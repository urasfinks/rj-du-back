package ru.jamsys;

import ru.jamsys.core.App;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.HttpAsyncResponse;
import ru.jamsys.core.promise.Promise;

import java.util.Map;

public class PromiseExtension {

    public static void addHandler(Promise promiseSource) {
        promiseSource.onComplete((_, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    Map<String, Object> output = promise.getRepositoryMap("output", Map.class, new HashMapBuilder<String, Object>());
                    output.put("status", true);
                    input.setBodyFromMap(output);
                    input.complete();
                })
                .onError((_, promise) -> {
                    App.error(promise.getException());
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    input.setError(promise.getException().getMessage());
                    input.complete();
                });
    }

}
