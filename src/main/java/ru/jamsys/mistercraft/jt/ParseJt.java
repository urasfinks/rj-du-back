package ru.jamsys.mistercraft.jt;

import ru.jamsys.jdbc.template.StatementType;
import ru.jamsys.jdbc.template.Template;
import ru.jamsys.jdbc.template.TemplateEnum;

public enum ParseJt implements TemplateEnum {

    SELECT("SELECT * from parse order by id", StatementType.SELECT_WITH_AUTO_COMMIT),
    INSERT("INSERT INTO parse (id, data) values (${IN.id::NUMBER}, ${IN.data::VARCHAR})", StatementType.SELECT_WITH_AUTO_COMMIT);

    private Template template;

    ParseJt(String sql, StatementType statementType) {
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
