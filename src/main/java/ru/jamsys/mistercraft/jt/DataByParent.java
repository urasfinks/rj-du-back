package ru.jamsys.mistercraft.jt;

import ru.jamsys.jdbc.template.StatementType;
import ru.jamsys.jdbc.template.Template;
import ru.jamsys.jdbc.template.TemplateEnum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataByParent implements TemplateEnum {

    String sql;
    Map<String, Object> arguments;

    public DataByParent(List<String> inListArg) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM data WHERE uuid_data IN (");
        arguments = new HashMap<>();
        int count = 0;
        for (String item : inListArg) {
            String key = "ARG_" + (count++);
            arguments.put(key, item);
            if (count > 1) {
                sb.append(", ");
            }
            sb.append("${IN.").append(key).append("::VARCHAR}");
        }
        sb.append(");");
        sql = sb.toString();
    }

    public Map<String, Object> getSqlArguments() {
        return arguments;
    }

    public String getSql() {
        return sql;
    }

    @Override
    public Template getTemplate() {
        try {
            return new Template(sql, StatementType.SELECT_WITH_AUTO_COMMIT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
