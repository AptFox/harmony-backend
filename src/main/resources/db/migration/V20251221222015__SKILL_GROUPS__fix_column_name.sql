/*
    TABLE: skill_groups
    CHANGES: fix column name to be snake_case
 */

ALTER TABLE skill_groups RENAME COLUMN imageurl TO image_url;
ALTER TABLE skill_groups RENAME COLUMN colorhex to color_hex;