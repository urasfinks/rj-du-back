package ru.jamsys.mistercraft.jt;

import ru.jamsys.jdbc.template.StatementType;
import ru.jamsys.jdbc.template.Template;
import ru.jamsys.jdbc.template.TemplateEnum;
import ru.jamsys.mistercraft.DataType;

public enum Data implements TemplateEnum {

    REMOVE("""
            UPDATE data SET
                is_remove_data = 1
            WHERE
                uuid_data = ${IN.uuid_data::VARCHAR}
                AND is_remove_data = 0
                AND (uuid_device_data = ${IN.uuid_device::VARCHAR} OR id_user = ${IN.id_user::NUMBER})
                AND type_data IN ('%s', '%s', '%s')
            """.formatted(
            DataType.blobRSync.name(),
            DataType.userDataRSync.name(),
            DataType.socket.name()
    ), StatementType.SELECT_WITH_AUTO_COMMIT),

    REMOVE_ALL("""
            UPDATE data SET
                is_remove_data = 1
            WHERE
                is_remove_data = 0
                AND (uuid_device_data = ${IN.uuid_device::VARCHAR} OR id_user = ${IN.id_user::NUMBER})
                AND type_data IN ('%s', '%s', '%s')
            """.formatted(
            DataType.blobRSync.name(),
            DataType.userDataRSync.name(),
            DataType.socket.name()
    ), StatementType.SELECT_WITH_AUTO_COMMIT),

    SELECT_MAX_REVISION_BY_TYPE("""
            SELECT
                type_data AS key,
                max(revision_data) AS max
            FROM data WHERE (type_data IN ('%s', '%s', '%s', '%s', '%s', '%s') AND id_user = 1 )
            OR ( type_data IN ('%s', '%s') AND id_user = ${IN.id_user::NUMBER})
            OR ( type_data = '%s' AND (uuid_device_data = ${IN.uuid_device::VARCHAR} OR id_user = ${IN.id_user::NUMBER}))
            GROUP BY type_data;
            """.formatted(

            DataType.template.name(),
            DataType.systemData.name(),
            DataType.js.name(),
            DataType.any.name(),
            DataType.json.name(),
            DataType.blob.name(),

            DataType.userDataRSync.name(),
            DataType.blobRSync.name(),

            DataType.socket.name()

    ), StatementType.SELECT_WITH_AUTO_COMMIT),

    SELECT_SYSTEM_DATA_RANGE("""
            SELECT
                uuid_data as uuid,
                parent_uuid_data as parent_uuid,
                trunc(extract(epoch from date_add_data)) as date_add,
                is_remove_data as is_remove,
                revision_data as revision,
                trunc(extract(epoch from date_update_data)) as date_update,
                value_data as value,
                key_data as key
            FROM data WHERE type_data = ${IN.type_data::VARCHAR}
            AND id_user = 1
            AND revision_data > ${IN.revision_data::NUMBER}
            ORDER BY revision_data ASC
            LIMIT 1000;
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    SELECT_USER_DATA_RANGE("""
            SELECT
                uuid_data as uuid,
                parent_uuid_data as parent_uuid,
                trunc(extract(epoch from date_add_data)) as date_add,
                is_remove_data as is_remove,
                revision_data as revision,
                trunc(extract(epoch from date_update_data)) as date_update,
                value_data as value,
                key_data as key
            FROM data WHERE type_data = ${IN.type_data::VARCHAR}
            AND id_user = ${IN.id_user::NUMBER}
            AND revision_data > ${IN.revision_data::NUMBER}
            ORDER BY revision_data ASC
            LIMIT 1000;
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    SELECT_SOCKET_DATA_RANGE("""
            SELECT
                uuid_data as uuid,
                parent_uuid_data as parent_uuid,
                trunc(extract(epoch from date_add_data)) as date_add,
                is_remove_data as is_remove,
                revision_data as revision,
                trunc(extract(epoch from date_update_data)) as date_update,
                value_data as value,
                key_data as key
            FROM data WHERE type_data = ${IN.type_data::VARCHAR}
            AND ( uuid_device_data = ${IN.uuid_device::VARCHAR} OR id_user = ${IN.id_user::NUMBER} )
            AND revision_data > ${IN.revision_data::NUMBER}
            ORDER BY revision_data ASC
            LIMIT 1000
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    SELECT("""
            SELECT * FROM data WHERE uuid_data = ${IN.uuid_data::VARCHAR}
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

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
            """, StatementType.CALL_WITH_AUTO_COMMIT),
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
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    //Получаем данные, которые привязаны к устройству либо персоне, что бы понять, что мы имеем вообще хоть какое-то право на их изменение
    CHECK_PERMISSION_SOCKET_DATA("""
            SELECT * FROM data
            WHERE
                type_data = '%s'
                AND (
                        uuid_data = ${IN.uuid_data::VARCHAR}
                        OR parent_uuid_data = ${IN.uuid_data::VARCHAR}
                )
                AND (
                        id_user = ${IN.id_user::NUMBER}
                        OR uuid_device_data = ${IN.uuid_device::VARCHAR}
                );
            """.formatted(DataType.socket.name()), StatementType.SELECT_WITH_AUTO_COMMIT),

    //Получить главную запись, где лежат данные
    GET_PRIMARY_SOCKET_DATA("""
            SELECT * FROM data
            WHERE
                type_data = '%s'
                AND uuid_data = ${IN.uuid_data::VARCHAR}
                FOR UPDATE;
            """.formatted(DataType.socket.name()), StatementType.SELECT_WITHOUT_AUTO_COMMIT),

    //Обновление данных только после проверки, что данные есть и они привязана к персоне или устройству
    UPDATE_PRIMARY_SOCKET_DATA("""
            UPDATE data SET value_data = ${IN.value_data::VARCHAR}
            WHERE id_data = ${IN.id_data::NUMBER};
            """, StatementType.SELECT_WITHOUT_AUTO_COMMIT),

    //Обновление ревизий у наследников
    UPDATE_SECONDARY_SOCKET_DATA("""
            UPDATE data SET value_data = null
            WHERE
                type_data = '%s'
                AND parent_uuid_data = ${IN.uuid_data::VARCHAR};
            """.formatted(DataType.socket.name()), StatementType.SELECT_WITH_AUTO_COMMIT),

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
            """, StatementType.SELECT_WITH_AUTO_COMMIT),

    UNLOCK("""
            COMMIT;
            """, StatementType.SELECT_WITHOUT_AUTO_COMMIT),
    LOCK("""
            SELECT * FROM data WHERE id_data = 82561 FOR UPDATE;
            """, StatementType.SELECT_WITHOUT_AUTO_COMMIT),

    UPDATE_ID_USER_BEFORE_SIGN_IN("""
            UPDATE data SET id_user = ${IN.id_user::NUMBER} WHERE id_user is null AND uuid_device_data = ${IN.uuid_device::VARCHAR}
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

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
