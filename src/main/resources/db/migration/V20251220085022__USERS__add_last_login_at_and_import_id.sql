/*
    TABLE: users
    CHANGES: adding last_login_at and import_id to users table
 */

ALTER TABLE users
    ADD last_login_at TIMESTAMP WITH TIME ZONE,
    ADD import_id VARCHAR(255);
