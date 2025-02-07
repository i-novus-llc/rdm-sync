INSERT INTO rdm_sync.source  (code, name, init_values, service_factory)
 VALUES('RDM', 'НСИ', null, 'test') ON CONFLICT(code) DO NOTHING;

INSERT INTO rdm_sync.source  (code, name, init_values, service_factory)
 VALUES('FNSI', 'ФНСИ', null, 'test2') ON CONFLICT(code) DO NOTHING;

