package ru.jamsys.mistercraft.jt;

import ru.jamsys.jdbc.template.StatementType;
import ru.jamsys.jdbc.template.Template;
import ru.jamsys.jdbc.template.TemplateEnum;

public enum User implements TemplateEnum {

    GET_BY_MAIL("SELECT * FROM \"user\" WHERE mail_user = lower(${IN.mail::VARCHAR});", StatementType.SELECT),

    GET_BY_CODE("SELECT * FROM \"user\" WHERE mail_user = lower(${IN.mail::VARCHAR}) AND code_user = ${IN.code::NUMBER};", StatementType.SELECT),

    RESET_CODE("UPDATE \"user\" set code_user = null WHERE mail_user = lower(${IN.mail::VARCHAR});", StatementType.SELECT),

    SET_CODE("UPDATE \"user\" set code_user = ${IN.code::NUMBER} WHERE mail_user = lower(${IN.mail::VARCHAR});", StatementType.SELECT),

    INSERT("INSERT INTO \"user\" (mail_user, code_user) VALUES (lower(${IN.mail::VARCHAR}), ${IN.code::NUMBER});", StatementType.SELECT);

    private Template template;

    User(String sql, StatementType statementType) {
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
