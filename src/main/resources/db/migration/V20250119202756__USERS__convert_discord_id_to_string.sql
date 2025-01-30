ALTER TABLE users
    DROP COLUMN discord_id;

ALTER TABLE users
    ADD discord_id VARCHAR(255);