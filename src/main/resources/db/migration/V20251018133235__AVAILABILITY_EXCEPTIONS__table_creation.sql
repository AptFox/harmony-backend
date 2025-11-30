CREATE TABLE availability_exceptions
(
    id BIGSERIAL NOT NULL,
    user_id UUID NOT NULL,
    player_id BIGINT,
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    comment VARCHAR(255)
);

CREATE INDEX idx_availability_exceptions_user_id ON availability_exceptions(user_id);
CREATE INDEX idx_availability_exceptions_player_id ON availability_exceptions(player_id);