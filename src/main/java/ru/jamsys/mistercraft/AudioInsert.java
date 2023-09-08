package ru.jamsys.mistercraft;

import ru.jamsys.App;
import ru.jamsys.Util;
import ru.jamsys.UtilBase64;
import ru.jamsys.UtilFile;
import ru.jamsys.mistercraft.jt.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AudioInsert {
    public static void parse() {
        System.out.println("Hello AudioInsert.parse()");
        String folder = "audio";
        List<String> filesRecursive = UtilFile.getFilesRecursive(folder);
        for (String pathFile : filesRecursive) {
            try {
                String base64Data = UtilBase64.base64Encode(UtilFile.readBytes(pathFile), false);
                Map<String, Object> arguments0 = App.jdbcTemplate.createArguments();
                String uuid = UtilFile.getFileName(pathFile);
                arguments0.put("uuid_data", uuid);
                List<Map<String, Object>> exec = App.jdbcTemplate.execute(App.postgresqlPoolName, Data.SELECT, arguments0);
                if (exec.isEmpty()) {
                    Map<String, Object> arguments = App.jdbcTemplate.createArguments();
                    arguments.put("uuid_data", uuid);
                    arguments.put("value_data", base64Data);
                    arguments.put("type_data", "blob");
                    arguments.put("parent_uuid_data", null);
                    arguments.put("date_add_data", new BigDecimal(System.currentTimeMillis()));
                    arguments.put("is_remove_data", 0);
                    arguments.put("id_user", 1);
                    arguments.put("key_data", "speech");
                    arguments.put("uuid_device", null);
                    arguments.put("new_id_revision", new BigDecimal(0));
                    App.jdbcTemplate.execute(App.postgresqlPoolName, Data.INSERT, arguments);
                }
                //System.out.println(exec);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //break;
        }
        System.out.println(filesRecursive);
    }
}
