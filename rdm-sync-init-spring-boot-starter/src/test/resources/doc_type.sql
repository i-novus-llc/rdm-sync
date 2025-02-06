CREATE SCHEMA IF NOT EXISTS rdm;

CREATE TABLE IF NOT EXISTS rdm.document_type
(
    parent_id integer,
    deleted_ts timestamp without time zone,
    name character varying COLLATE pg_catalog."default",
    actual boolean,
    code integer
);