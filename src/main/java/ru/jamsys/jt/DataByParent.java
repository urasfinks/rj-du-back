package ru.jamsys.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

public enum DataByParent implements JdbcRequestRepository {

    PROCESSED("""
            SELECT * FROM data WHERE uuid_data IN (${IN.processed::IN_ENUM_VARCHAR});
            """, StatementType.SELECT_WITH_AUTO_COMMIT);


    private JdbcTemplate template;

    DataByParent(String sql, StatementType statementType) {
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
