/*
    TABLE: organizations
    CHANGES: Adding time_zone_id
 */

ALTER TABLE organizations
    ADD time_zone_id VARCHAR(255);

--- insert MLE TZ
DO $$
    DECLARE
        mle_org_id BIGINT := (SELECT id FROM organizations WHERE acronym = 'MLE');
    BEGIN
        UPDATE organizations SET time_zone_id = 'America/New_York' WHERE id = mle_org_id;
    END $$;