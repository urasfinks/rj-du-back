-- Table: public.user

-- DROP TABLE IF EXISTS public."user";

CREATE TABLE IF NOT EXISTS public."user"
(
    id_user bigserial NOT NULL,
    mail_user character varying(255) COLLATE pg_catalog."default" NOT NULL,
    date_add_user timestamp without time zone NOT NULL DEFAULT (now())::timestamp without time zone,
    code_user integer,
    CONSTRAINT user_pkey PRIMARY KEY (id_user)
);

-- Table: public.device

-- DROP TABLE IF EXISTS public.device;

CREATE TABLE IF NOT EXISTS public.device
(
    uuid_device character varying(255) COLLATE pg_catalog."default" NOT NULL,
    id_user bigint,
    date_add_device timestamp without time zone NOT NULL DEFAULT (now())::timestamp without time zone,
    CONSTRAINT device_pkey PRIMARY KEY (uuid_device),
    CONSTRAINT fkey01_device FOREIGN KEY (id_user)
        REFERENCES public."user" (id_user) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

-- Table: public.data

-- DROP TABLE IF EXISTS public.data;

CREATE TABLE IF NOT EXISTS public.data
(
    id_data bigserial NOT NULL,
    uuid_data character varying(255) COLLATE pg_catalog."default" NOT NULL,
    type_data character varying(255) COLLATE pg_catalog."default" NOT NULL,
    parent_uuid_data character varying(255) COLLATE pg_catalog."default",
    revision_data bigint NOT NULL DEFAULT 0,
    date_add_data timestamp without time zone NOT NULL DEFAULT (now())::timestamp without time zone,
    is_remove_data integer NOT NULL DEFAULT 0,
    id_user bigint,
    key_data character varying COLLATE pg_catalog."default",
    meta_data character varying(255) COLLATE pg_catalog."default",
    date_update_data timestamp without time zone NOT NULL DEFAULT (now())::timestamp without time zone,
    uuid_device_data character varying(255) COLLATE pg_catalog."default",
    lazy_sync_data character varying(255) COLLATE pg_catalog."default",
    value_data text COLLATE pg_catalog."default",
    CONSTRAINT data_pkey PRIMARY KEY (id_data),
    CONSTRAINT uniq01_uuid_data UNIQUE (uuid_data, id_user),
    CONSTRAINT fkey01_data FOREIGN KEY (id_user)
        REFERENCES public."user" (id_user) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
        NOT VALID
);

-- Index: idx01_data

-- DROP INDEX IF EXISTS public.idx01_data;

CREATE UNIQUE INDEX IF NOT EXISTS idx01_data
    ON public.data USING btree
    (uuid_data COLLATE pg_catalog."default" ASC NULLS LAST, id_user ASC NULLS LAST)
    TABLESPACE pg_default;
-- Index: idx02_data

-- DROP INDEX IF EXISTS public.idx02_data;

CREATE INDEX IF NOT EXISTS idx02_data
    ON public.data USING btree
    (parent_uuid_data COLLATE pg_catalog."default" ASC NULLS LAST)
    TABLESPACE pg_default;
-- Index: idx03_data

-- DROP INDEX IF EXISTS public.idx03_data;

CREATE INDEX IF NOT EXISTS idx03_data
    ON public.data USING btree
    (id_user ASC NULLS LAST, revision_data ASC NULLS LAST, type_data COLLATE pg_catalog."default" ASC NULLS LAST)
    TABLESPACE pg_default;
-- Index: idx04_data

-- DROP INDEX IF EXISTS public.idx04_data;

CREATE INDEX IF NOT EXISTS idx04_data
    ON public.data USING btree
    (uuid_device_data COLLATE pg_catalog."default" ASC NULLS LAST)
    TABLESPACE pg_default;

-- FUNCTION: public.update_data_trg()

-- DROP FUNCTION IF EXISTS public.update_data_trg();

CREATE OR REPLACE FUNCTION public.update_data_trg()
    RETURNS trigger
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE NOT LEAKPROOF
AS $BODY$
    BEGIN
        IF (TG_OP = 'DELETE') THEN

			UPDATE data SET
				is_remove_data = 1,
				date_update_data = now()::timestamp,
				revision_data = nextval('revision'::regclass)
			WHERE id_data = OLD.id_data;

            RETURN null;
        ELSIF (TG_OP = 'UPDATE') THEN
		    -- мы не должны останавливать операцию если is_remove = 1
			-- так как есть процесс восстановления информации account/avatar
			NEW.date_update_data := now()::timestamp;
			NEW.revision_data := nextval('revision'::regclass);

            RETURN NEW;
        ELSIF (TG_OP = 'INSERT') THEN

			NEW.revision_data := nextval('revision'::regclass);

            RETURN NEW;
        END IF;
    END;
$BODY$;

-- Trigger: trg01_data

-- DROP TRIGGER IF EXISTS trg01_data ON public.data;

CREATE TRIGGER trg01_data
    BEFORE INSERT OR DELETE OR UPDATE OF uuid_data, type_data, parent_uuid_data, is_remove_data, key_data, meta_data, value_data
    ON public.data
    FOR EACH ROW
    EXECUTE FUNCTION public.update_data_trg();

-- PROCEDURE: public.add_data(character varying, text, character varying, character varying, bigint, integer, bigint, character varying, character varying, character varying)

-- DROP PROCEDURE IF EXISTS public.add_data(character varying, text, character varying, character varying, bigint, integer, bigint, character varying, character varying, character varying);

CREATE OR REPLACE PROCEDURE public.add_data(
	IN p_uuid_data character varying,
	IN p_value_data text,
	IN p_type_data character varying,
	IN p_parent_uuid_data character varying,
	IN p_date_add_data bigint,
	IN p_is_remove_data integer,
	IN p_id_user bigint,
	IN p_key_data character varying,
	IN p_meta_data character varying,
	IN p_uuid_device_data character varying,
	OUT p_new_revision_data character varying)
LANGUAGE 'plpgsql'
AS $BODY$
DECLARE
	idData bigint;
	typeData character varying(255);
	newRevisionData bigint;
BEGIN
	--p_new_revision_data := 0;
	SELECT id_data, type_data INTO idData, typeData FROM "data" d WHERE d.uuid_data = p_uuid_data AND d.id_user = p_id_user;
	IF idData > 0 THEN
		--Обновляем только в случаи если пользователь является владельцем по учётке или устройству
		IF typeData != 'socket' THEN --Сокетные данные надо обновлять через rest api
			UPDATE "data" SET
				value_data = p_value_data,
				key_data = p_key_data,
				meta_data = p_meta_data,
				is_remove_data = 0 --Случай удаления пользователя, и повторная запись account/avatar
			WHERE id_data = idData
			AND (uuid_device_data = p_uuid_device_data OR id_user = p_id_user);

			SELECT revision_data INTO newRevisionData FROM "data"
			WHERE id_data = idData
			AND (uuid_device_data = p_uuid_device_data OR id_user = p_id_user);
			p_new_revision_data := newRevisionData || '';
		END IF;
	ELSE
		INSERT INTO "data" (uuid_data, value_data, type_data, parent_uuid_data, date_add_data, is_remove_data, id_user, key_data, meta_data, uuid_device_data)
		VALUES (
			p_uuid_data,
			p_value_data,
			p_type_data,
			p_parent_uuid_data,
			to_timestamp(p_date_add_data)::timestamp without time zone,
			p_is_remove_data,
			p_id_user,
			p_key_data,
			p_meta_data,
			p_uuid_device_data
		) RETURNING revision_data INTO newRevisionData;
		p_new_revision_data := newRevisionData || '';
	END IF;
END;
$BODY$;
