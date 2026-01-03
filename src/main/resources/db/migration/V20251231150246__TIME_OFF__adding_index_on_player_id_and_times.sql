/*
    TABLE: time_off
    CHANGES: adding composite index on player_id and times
 */

CREATE INDEX idx_time_off_player_id_start_time ON time_off(player_id, start_time);
CREATE INDEX idx_time_off_player_id_end_time ON time_off(player_id, end_time);

ALTER TABLE time_off
    RENAME CONSTRAINT availability_exceptions_pkey TO time_off_pkey;