package ru.jamsys.promise.repository;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.builder.HashMapBuilder;

@Getter
@Setter
public class AuthRepository {

    String uuidDevice;

    Long idUser;

    public HashMapBuilder<String, Object> get() {
        return new HashMapBuilder<String, Object>()
                .append("id_user", idUser)
                .append("uuid_device", uuidDevice);
    }

}
