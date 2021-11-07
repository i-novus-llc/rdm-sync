CREATE TABLE IF NOT EXISTS ref_ek001(
    "name" character varying,
    "id" integer,
    "is_deleted" boolean,
    "rdm_sync_internal_local_row_state" character varying NOT NULL DEFAULT 'DIRTY'::character varying
);

CREATE TABLE IF NOT EXISTS ref_ek002(
    "name" character varying,
    "id" integer,
    "is_deleted" boolean,
    "rdm_sync_internal_local_row_state" character varying NOT NULL DEFAULT 'DIRTY'::character varying
);