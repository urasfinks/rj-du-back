package ru.jamsys.mistercraft.jt;

import ru.jamsys.jdbc.template.StatementType;
import ru.jamsys.jdbc.template.Template;
import ru.jamsys.jdbc.template.TemplateEnum;

public enum Device implements TemplateEnum {

    SELECT_BY_UUID("SELECT * FROM device WHERE uuid_device = ${IN.uuid_device::VARCHAR}", StatementType.SELECT),

    SELECT_BY_ID_USER("SELECT * FROM device WHERE id_user = ${IN.id_user::NUMBER}", StatementType.SELECT),

    UPDATE_ID_USER("UPDATE device SET id_user = ${IN.id_user::NUMBER} WHERE uuid_device = ${IN.uuid_device::VARCHAR}", StatementType.SELECT),

    INSERT("INSERT INTO device (uuid_device) values (${IN.uuid_device::VARCHAR})", StatementType.SELECT);

    private Template template;

    Device(String sql, StatementType statementType) {
        try {
            template = new Template(sql, statementType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Template getTemplate() {
        return template;
    }
}
