/*
    TABLE: users
    CHANGES: remove uq_users_import_id
 */

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS uq_users_import_id;