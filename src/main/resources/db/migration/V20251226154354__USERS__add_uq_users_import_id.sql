/*
    TABLE: users
    CHANGES: add uq_users_import_id and comment
 */

ALTER TABLE users
    ADD CONSTRAINT uq_users_import_id UNIQUE (import_id);

COMMENT ON COLUMN users.import_id IS 'Stores the UID from data_source import. Used to link players to users.'