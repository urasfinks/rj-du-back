package ru.jamsys.mistercraft.handler.socket;

import ru.jamsys.App;
import ru.jamsys.UtilJson;
import ru.jamsys.component.JdbcTemplate;
import ru.jamsys.mistercraft.ControllerWebSocket;
import ru.jamsys.mistercraft.jt.Data;
import ru.jamsys.mistercraft.socket.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Broadcast implements SocketHandler {

    private final ControllerWebSocket controllerWebSocket;
    public static JdbcTemplate jdbcTemplate;

    public Broadcast() {
        controllerWebSocket = App.context.getBean(ControllerWebSocket.class);
        jdbcTemplate = App.context.getBean(JdbcTemplate.class);
    }

    @Override
    public void handler(Request request) {
        try {
            //request.getUuidData(); // этот uuid - должен являться PRIMARY сокетных данных
            Map<String, Object> arguments = jdbcTemplate.createArguments();
            arguments.put("uuid_data", request.getUuidData());
            List<Map<String, Object>> exec = jdbcTemplate.exec(App.postgreSQLPoolName, Data.GET_SOCKET_UUID_DEVICE, arguments);
            List<String> listUUidDevice = new ArrayList<>();
            for (Map<String, Object> item : exec) {
                listUUidDevice.add((String) item.get("uuid"));
            }
            controllerWebSocket.sendByUuidDevice(listUUidDevice, UtilJson.toString(request.getData(), "{}"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
