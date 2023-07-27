package ru.jamsys.mistercraft.jt;

import ru.jamsys.jdbc.template.StatementType;
import ru.jamsys.jdbc.template.Template;
import ru.jamsys.jdbc.template.TemplateEnum;

public enum Data implements TemplateEnum {

    SELECT_MAX_REVISION_BY_TYPE("""
            SELECT
                type_data AS key,
                max(revision_data) AS max
            FROM data WHERE (type_data IN ('template', 'systemData', 'js', 'any') AND id_user = 1 )
            OR ( type_data = 'userDataRSync' AND id_user = ${IN.id_user::NUMBER} )
            OR ( type_data = 'socket' AND uuid_device_data = ${IN.uuid_device::VARCHAR} )
            GROUP BY type_data;
            """, StatementType.SELECT),

    SELECT_SYSTEM_DATA_RANGE("""
            SELECT
                uuid_data as uuid,
                parent_uuid_data as parent_uuid,
                date_add_data as date_add,
                is_remove_data as is_remove,
                revision_data as revision,
                date_update_data as date_update,
                value_data as value,
                key_data as key
            FROM data WHERE type_data = ${IN.type_data::VARCHAR}
            AND id_user = 1
            AND revision_data > ${IN.revision_data::NUMBER}
            ORDER BY revision_data ASC
            LIMIT 1000;
            """, StatementType.SELECT),

    SELECT_USER_DATA_RANGE("""
            SELECT
                uuid_data as uuid,
                parent_uuid_data as parent_uuid,
                date_add_data as date_add,
                is_remove_data as is_remove,
                revision_data as revision,
                date_update_data as date_update,
                value_data as value,
                key_data as key
            FROM data WHERE type_data = ${IN.type_data::VARCHAR}
            AND id_user = ${IN.id_user::NUMBER}
            AND revision_data > ${IN.revision_data::NUMBER}
            ORDER BY revision_data ASC
            LIMIT 1000;
            """, StatementType.SELECT),

    SELECT_SOCKET_DATA_RANGE("""
            SELECT
                uuid_data as uuid,
                parent_uuid_data as parent_uuid,
                date_add_data as date_add,
                is_remove_data as is_remove,
                revision_data as revision,
                date_update_data as date_update,
                value_data as value,
                key_data as key
            FROM data WHERE type_data = ${IN.type_data::VARCHAR}
            AND uuid_device_data = ${IN.uuid_device::VARCHAR}
            AND revision_data > ${IN.revision_data::NUMBER}
            ORDER BY revision_data ASC
            LIMIT 1000
            """, StatementType.SELECT),

    INSERT("""
            CALL add_data(
            	${IN.uuid_data::VARCHAR},
                ${IN.value_data::VARCHAR}::text,
                ${IN.type_data::VARCHAR},
                ${IN.parent_uuid_data::VARCHAR},
                ${IN.date_add_data::NUMBER}::bigint,
                ${IN.is_remove_data::NUMBER}::integer,
                ${IN.id_user::NUMBER}::bigint,
                ${IN.key_data::VARCHAR},
                ${IN.uuid_device::VARCHAR},
                ${OUT.new_id_revision::VARCHAR}
            );
            """, StatementType.CALL),
    TEST_INSERT("""
            INSERT INTO data (
                uuid_data,
                value_data,
                type_data,
                parent_uuid_data,
                id_user,
                key_data,
                uuid_device_data
            ) VALUES (
                ${IN.uuid_data::VARCHAR},
                ${IN.value_data::VARCHAR},
                ${IN.type_data::VARCHAR},
                ${IN.parent_uuid_data::VARCHAR},
                ${IN.id_user::NUMBER},
                ${IN.key_data::VARCHAR},
                ${IN.uuid_device_data::VARCHAR}
            );
            """, StatementType.SELECT),

    //Получаем данные, которые привязаны к устройству либо персоне, что бы понять, что мы имеем вообще хоть какое-то право на их изменение
    CHECK_PERMISSION_SOCKET_DATA("""
            SELECT * FROM data
            WHERE
                type_data = 'socket'
                AND (
                        uuid_data = ${IN.uuid_data::VARCHAR}
                        OR parent_uuid_data = ${IN.uuid_data::VARCHAR}
                )
                AND (
                        id_user = ${IN.id_user::NUMBER}
                        OR uuid_device_data = ${IN.uuid_device::VARCHAR}
                );
            """, StatementType.SELECT),

    //Получить главную запись, где лежат данные
    GET_PRIMARY_SOCKET_DATA("""
            SELECT * FROM data
            WHERE
                type_data = 'socket'
                AND uuid_data = ${IN.uuid_data::VARCHAR}
                FOR UPDATE;
            """, StatementType.SELECT),

    //Обновление данных только после проверки, что данные есть и они привязана к персоне или устройству
    UPDATE_PRIMARY_SOCKET_DATA("""
            UPDATE data SET value_data = ${IN.value_data::VARCHAR}
            WHERE id_data = ${IN.id_data::NUMBER};
            """, StatementType.SELECT),

    //Обновление ревизий у наследников
    UPDATE_SECONDARY_SOCKET_DATA("""
            UPDATE data SET value_data = null
            WHERE
                type_data = 'socket'
                AND parent_uuid_data = ${IN.uuid_data::VARCHAR};
            """, StatementType.SELECT),

    //Получить все uuid_device причастные к PRIMARY сокет данным
    GET_SOCKET_UUID_DEVICE("""
            SELECT DISTINCT ON(uuid) * FROM (
                --Выборка uuid_device для авторизованных через привязку данных к user
            	SELECT dv.uuid_device as uuid FROM "data" da
            	INNER JOIN device dv ON da.id_user = dv.id_user
            	WHERE da.uuid_data = ${IN.uuid_data::VARCHAR} OR da.parent_uuid_data = ${IN.uuid_data::VARCHAR}
            	UNION ALL
            	--Выборка uuid_device для не авторизованных
            	SELECT uuid_device_data as uuid FROM "data" da
            	WHERE da.uuid_data = ${IN.uuid_data::VARCHAR} OR da.parent_uuid_data = ${IN.uuid_data::VARCHAR}
            ) as sq1
            """, StatementType.SELECT),

    UNLOCK("""
            COMMIT;
            """, StatementType.SELECT),
    LOCK("""
            SELECT * FROM data WHERE id_data = 82561 FOR UPDATE;
            """, StatementType.SELECT);

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
