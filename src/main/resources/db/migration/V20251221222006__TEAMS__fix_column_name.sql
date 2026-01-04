/*
    TABLE: teams
    CHANGES: fix column name to be snake_case
 */

ALTER TABLE teams
    RENAME COLUMN imageurl TO image_url