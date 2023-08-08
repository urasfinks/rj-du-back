package ru.jamsys.mistercraft.jt;

import ru.jamsys.jdbc.template.StatementType;
import ru.jamsys.jdbc.template.Template;
import ru.jamsys.jdbc.template.TemplateEnum;

public enum Device implements TemplateEnum {

    SELECT_BY_UUID("SELECT * FROM device WHERE uuid_device = ${IN.uuid_device::VARCHAR}", StatementType.SELECT_WITH_AUTO_COMMIT),

    SELECT_BY_ID_USER("SELECT * FROM device WHERE id_user = ${IN.id_user::NUMBER}", StatementType.SELECT_WITH_AUTO_COMMIT),

    UPDATE_ID_USER("UPDATE device SET id_user = ${IN.id_user::NUMBER} WHERE uuid_device = ${IN.uuid_device::VARCHAR}", StatementType.SELECT_WITH_AUTO_COMMIT),

    UPDATE("UPDATE device SET id_user = ${IN.id_user::NUMBER} WHERE uuid_device = ${IN.uuid_device::VARCHAR}", StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("INSERT INTO device (uuid_device, id_user) values (${IN.uuid_device::VARCHAR}, ${IN.id_user::NUMBER})", StatementType.SELECT_WITH_AUTO_COMMIT);

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
