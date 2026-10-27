/*
    TABLE: players
    CHANGES:
        add imported_at to track when each player was last seen in an import,
        backfill existing players so the first cleanup doesn't treat them as stale
 */

ALTER TABLE players
    ADD imported_at TIMESTAMP WITH TIME ZONE;

UPDATE players SET imported_at = NOW();
