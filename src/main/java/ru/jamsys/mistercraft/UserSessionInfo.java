package ru.jamsys.mistercraft;

import lombok.Data;
import ru.jamsys.App;
import ru.jamsys.mistercraft.jt.Device;

import java.util.List;
import java.util.Map;

@Data
public class UserSessionInfo {

    int version;
    private Long idUser;
    private String deviceUuid;
    private boolean checked = false;

    public boolean isRegister() {
        return isValidRequest() && idUser > 0;
    }

    public boolean isValidRequest() {
        return version > 0 && deviceUuid != null;
    }

    public void appendAuthJdbcTemplateArguments(Map<String, Object> arguments) {
        arguments.put("id_user", idUser);
        arguments.put("uuid_device", deviceUuid);
    }

    public void check() {
        if (!checked) {
            checked = true;
            Map<String, Object> arguments = App.jdbcTemplate.createArguments();
            arguments.put("uuid_device", deviceUuid);
            try {
                List<Map<String, Object>> exec = App.query(Device.SELECT_BY_UUID, arguments);
                if (exec.size() > 0) {
                    Object idUser = exec.get(0).get("id_user");
                    if (idUser != null) {
                        this.idUser = (long) idUser;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
