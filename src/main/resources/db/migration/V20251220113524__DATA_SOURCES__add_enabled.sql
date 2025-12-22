/*
    TABLE: data_sources
    CHANGES: adding enabled field to data_sources table
 */

ALTER TABLE data_sources
    ADD enabled BOOLEAN DEFAULT FALSE;

DO $$
    DECLARE
        mle_org_id BIGINT := (SELECT id FROM organizations WHERE acronym = 'MLE');
    BEGIN
        UPDATE data_sources SET enabled = TRUE WHERE org_id = mle_org_id;
    END $$;