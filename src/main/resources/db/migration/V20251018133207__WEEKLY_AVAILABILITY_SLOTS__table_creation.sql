CREATE TABLE weekly_availability_slots
(
    id BIGSERIAL NOT NULL,
    user_id UUID NOT NULL,
    player_id BIGINT,
    day_of_week VARCHAR(3),
    start_time TIME WITHOUT TIME ZONE,
    end_time TIME WITHOUT TIME ZONE,
    time_zone_id VARCHAR(255)
);

CREATE INDEX idx_weekly_availability_slots_user_id ON weekly_availability_slots(user_id);
CREATE INDEX idx_weekly_availability_slots_player_id ON weekly_availability_slots(player_id);
