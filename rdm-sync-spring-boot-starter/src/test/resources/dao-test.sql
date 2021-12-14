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

CREATE TABLE IF NOT EXISTS ref_cars(
    "model" character varying,
    "id" integer,
    "deleted_ts" timestamp without time zone,
    "rdm_sync_internal_local_row_state" character varying NOT NULL DEFAULT 'DIRTY'::character varying
);

INSERT INTO ref_cars (model, id, deleted_ts, rdm_sync_internal_local_row_state) VALUES ('model-1', 1, '2021-09-25 12:30:00', 'SYNCED');
INSERT INTO ref_cars (model, id, deleted_ts, rdm_sync_internal_local_row_state) VALUES ('model-2', 2, NULL, 'SYNCED');

INSERT INTO rdm_sync.source (name, code, init_values) VALUES ('name1', 'CODE-1', '{}');
INSERT INTO rdm_sync.source (name, code, init_values) VALUES ('name2', 'CODE-2', '{}');

INSERT INTO rdm_sync.mapping(
	sys_table, unique_sys_field, deleted_field, mapping_version, mapping_last_updated)
	VALUES ('test_table', 'id', 'deleted_ts', -1, now());
INSERT INTO rdm_sync.refbook(
	code, name, source_id, sync_type, start_version)
	VALUES ('testCode', 'testName', (SELECT id FROM rdm_sync.source WHERE code = 'CODE-1'), 'NOT_VERSIONED', null);
INSERT INTO rdm_sync.version(
	version, mapping_id, ref_id)
	VALUES ('CURRENT', (SELECT id FROM rdm_sync.mapping WHERE sys_table = 'test_table'), (SELECT id FROM rdm_sync.refbook WHERE code = 'testCode'));

