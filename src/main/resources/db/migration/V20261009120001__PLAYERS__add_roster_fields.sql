/*
    TABLE: players
    CHANGES:
        add uq_players_user_org,
        add salary, slot, current_scrim_points and eligible_through
 */

ALTER TABLE players
    ADD CONSTRAINT uq_players_user_org UNIQUE (user_id, org_id),
    ADD salary VARCHAR(32),
    ADD slot VARCHAR(32),
    ADD current_scrim_points INTEGER,
    ADD eligible_through VARCHAR(32);
