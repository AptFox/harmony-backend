/*
    TABLE: teams
    CHANGES: remove acronym and image_url columns
 */

ALTER TABLE teams
    DROP COLUMN acronym;

ALTER TABLE teams
    DROP COLUMN image_url;