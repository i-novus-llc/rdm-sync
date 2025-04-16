CREATE SCHEMA IF NOT EXISTS rdm_sync;
CREATE TABLE rdm_sync.refbook (
    code varchar
);
INSERT INTO rdm_sync.refbook(code) VALUES('test');


CREATE TABLE IF NOT EXISTS rdm_sync.loaded_version
(
    id integer NOT NULL,
    code character varying(50) NOT NULL,
    version character varying(50),
    publication_dt timestamp without time zone,
    load_dt timestamp without time zone,
    close_dt timestamp without time zone,
    is_actual boolean,
    CONSTRAINT loaded_version_pk PRIMARY KEY (id)
);

INSERT INTO rdm_sync.loaded_version(
	id, code, version, publication_dt, load_dt, is_actual)
	VALUES (1, 'test', '1', now(), now(), true);

INSERT INTO rdm_sync.loaded_version(
	id, code, version, publication_dt, load_dt, is_actual)
	VALUES (2, 'test', '2', now(), now(), true);

INSERT INTO rdm_sync.loaded_version(
	id, code, version, publication_dt, load_dt, is_actual)
	VALUES (3, 'test', '3', now(), now(), true);

