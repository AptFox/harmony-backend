CREATE TABLE users
(
    user_id      UUID NOT NULL,
    display_name VARCHAR(255),
    discord_id   INTEGER,
    time_zone_id VARCHAR(255),
    player_id    INTEGER,
    role_id      INTEGER,
    CONSTRAINT pk_users PRIMARY KEY (user_id)
);