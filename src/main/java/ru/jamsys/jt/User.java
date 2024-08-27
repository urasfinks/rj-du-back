package ru.jamsys.jt;


import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;

public enum User implements JdbcRequestRepository {

    GET_BY_MAIL("SELECT * FROM \"user\" WHERE mail_user = lower(${IN.mail::VARCHAR});", StatementType.SELECT_WITH_AUTO_COMMIT),

    GET_BY_CODE_APPLE_REVIEW("SELECT * FROM \"user\" WHERE mail_user = lower(${IN.mail::VARCHAR});", StatementType.SELECT_WITH_AUTO_COMMIT),

    GET_BY_CODE("SELECT * FROM \"user\" WHERE mail_user = lower(${IN.mail::VARCHAR}) AND code_user = ${IN.code::NUMBER};", StatementType.SELECT_WITH_AUTO_COMMIT),

    RESET_CODE("UPDATE \"user\" set code_user = null WHERE mail_user = lower(${IN.mail::VARCHAR});", StatementType.SELECT_WITH_AUTO_COMMIT),

    SET_CODE("UPDATE \"user\" set code_user = ${IN.code::NUMBER} WHERE mail_user = lower(${IN.mail::VARCHAR});", StatementType.SELECT_WITH_AUTO_COMMIT),

    INSERT("INSERT INTO \"user\" (mail_user, code_user) VALUES (lower(${IN.mail::VARCHAR}), ${IN.code::NUMBER});", StatementType.SELECT_WITH_AUTO_COMMIT);

    private JdbcTemplate template;

    User(String sql, StatementType statementType) {
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
