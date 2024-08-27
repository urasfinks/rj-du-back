package ru.jamsys.jt;


import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

public enum ParseJt implements JdbcRequestRepository {

    SELECT("SELECT * from parse order by id", StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("INSERT INTO parse (id, data) values (${IN.id::NUMBER}, ${IN.data::VARCHAR})", StatementType.SELECT_WITH_AUTO_COMMIT);

    private JdbcTemplate template;

    ParseJt(String sql, StatementType statementType) {
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
