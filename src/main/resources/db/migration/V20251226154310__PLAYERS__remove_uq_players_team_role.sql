/*
    TABLE: players
    CHANGES: remove uq_players_team_role
 */

ALTER TABLE players
    DROP CONSTRAINT IF EXISTS uq_players_team_role;