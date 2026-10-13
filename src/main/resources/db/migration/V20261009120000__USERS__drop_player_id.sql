/*
    TABLE: users
    CHANGES: drop player_id (superseded by players.user_id)
 */

ALTER TABLE users DROP COLUMN player_id;
