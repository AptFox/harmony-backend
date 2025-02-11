ALTER TABLE users
    DROP COLUMN time_zone_id;

ALTER TABLE users
    ADD time_zone_id INTEGER;