CREATE SCHEMA IF NOT EXISTS rdm_sync;
CREATE TABLE rdm_sync.refbook (
    code varchar
);
INSERT INTO rdm_sync.refbook(code) VALUES('test');