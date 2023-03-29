package ru.jamsys.mistercraft.jt;

import ru.jamsys.jdbc.template.StatementType;
import ru.jamsys.jdbc.template.Template;
import ru.jamsys.jdbc.template.TemplateEnum;

public enum Data implements TemplateEnum {

    SELECT_SHARE_MAX_REVISION_BY_TYPE("SELECT type_data, max(revision_data) AS max " +
            "FROM data WHERE type_data NOT IN ('state', 'userData') " +
            "GROUP BY type_data", StatementType.SELECT),

    SELECT_USER_MAX_REVISION_BY_TYPE("SELECT type_data, max(revision_data) AS max " +
            "FROM data WHERE type_data IN ('state', 'userData') " +
            "AND id_user = ${IN.id_user::NUMBER} " +
            "GROUP BY type_data", StatementType.SELECT),

    SELECT_SHARE_DATA_RANGE("SELECT uuid_data as uuid,\n" +
            "                            parent_uuid_data as parent_uuid,\n" +
            "                            date_add_data as date_add,\n" +
            "                            is_remove_data as is_remove,\n" +
            "                            revision_data as revision,\n" +
            "                            date_update_data as date_update,\n" +
            "                            value_data as value,\n" +
            "                            key_data as key\n" +
            " FROM data WHERE type_data = ${IN.type_data::VARCHAR} " +
            "AND id_user = 1 " +
            "AND revision_data > ${IN.revision_data::NUMBER} " +
            "ORDER BY revision_data ASC " +
            "LIMIT 1000", StatementType.SELECT),

    SELECT_USER_DATA_RANGE("SELECT uuid_data as uuid,\n" +
            "                            parent_uuid_data as parent_uuid,\n" +
            "                            date_add_data as date_add,\n" +
            "                            is_remove_data as is_remove,\n" +
            "                            revision_data as revision,\n" +
            "                            date_update_data as date_update,\n" +
            "                            value_data as value,\n" +
            "                            key_data as key\n" +
            " FROM data WHERE type_data = ${IN.type_data::VARCHAR} " +
            "AND id_user = ${IN.id_user::NUMBER} " +
            "AND revision_data > ${IN.revision_data::NUMBER} " +
            "ORDER BY revision_data ASC " +
            "LIMIT 1000", StatementType.SELECT),

    INSERT("INSERT INTO data (" +
            "    uuid_data, " +
            "    value_data, " +
            "    type_data, " +
            "    parent_uuid_data, " +
            "    id_user, " +
            "    key_data" +
            ") VALUES ( " +
            "    ${IN.uuid_data::VARCHAR}, " +
            "    ${IN.value_data::VARCHAR}, " +
            "    ${IN.type_data::VARCHAR}, " +
            "    ${IN.parent_uuid_data::VARCHAR}, " +
            "    ${IN.id_user::NUMBER}, " +
            "    ${IN.key_data::VARCHAR} " +
            ")",StatementType.SELECT);

    private Template template;

    Data(String sql, StatementType statementType) {
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
