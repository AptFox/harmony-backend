/*
    TABLE: user
    CHANGES: Adding fkey on player_id,
        Adding index on import_id and player_id,
        Adding unique constraint on discord_id
 */

ALTER TABLE users
    ALTER COLUMN player_id TYPE BIGINT,
    ADD CONSTRAINT fk_user_player FOREIGN KEY (player_id) REFERENCES players(id),
    ADD CONSTRAINT uq_discord_id UNIQUE (discord_id);


CREATE INDEX idx_users_import_id ON users(import_id);
CREATE INDEX idx_users_player_id ON users(player_id);
