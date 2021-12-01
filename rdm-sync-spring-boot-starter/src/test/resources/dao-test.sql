CREATE TABLE IF NOT EXISTS ref_ek001(
    "name" character varying,
    "id" integer,
    "deleted_ts" timestamp without time zone,
    "rdm_sync_internal_local_row_state" character varying NOT NULL DEFAULT 'DIRTY'::character varying
);

CREATE TABLE IF NOT EXISTS ref_ek002(
    "name" character varying,
    "id" integer,
    "deleted_ts" timestamp without time zone,
    "rdm_sync_internal_local_row_state" character varying NOT NULL DEFAULT 'DIRTY'::character varying
);

INSERT INTO rdm_sync.source (name, code, init_values) VALUES ('name1', 'CODE-1', '{}');
INSERT INTO rdm_sync.source (name, code, init_values) VALUES ('name2', 'CODE-2', '{}');

