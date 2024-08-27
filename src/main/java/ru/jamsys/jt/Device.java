package ru.jamsys.jt;


import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

public enum Device implements JdbcRequestRepository {

    SELECT_BY_UUID("SELECT * FROM device WHERE uuid_device = ${IN.uuid_device::VARCHAR}", StatementType.SELECT_WITH_AUTO_COMMIT),

    SELECT_BY_ID_USER("SELECT * FROM device WHERE id_user = ${IN.id_user::NUMBER}", StatementType.SELECT_WITH_AUTO_COMMIT),

    UPDATE_ID_USER("UPDATE device SET id_user = ${IN.id_user::NUMBER} WHERE uuid_device = ${IN.uuid_device::VARCHAR}", StatementType.SELECT_WITH_AUTO_COMMIT),

    UPDATE("UPDATE device SET id_user = ${IN.id_user::NUMBER} WHERE uuid_device = ${IN.uuid_device::VARCHAR}", StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("INSERT INTO device (uuid_device, id_user) values (${IN.uuid_device::VARCHAR}, ${IN.id_user::NUMBER})", StatementType.SELECT_WITH_AUTO_COMMIT);

    private JdbcTemplate template;

    Device(String sql, StatementType statementType) {
        try {
            template = new JdbcTemplate(sql, statementType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return template;
    }
}
