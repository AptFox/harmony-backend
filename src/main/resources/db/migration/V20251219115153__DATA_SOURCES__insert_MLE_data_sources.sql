/*
    TABLE: data_sources
    CHANGES: inserting MLE data sources
 */

-- insert MLE data sources
DO $$
    DECLARE
        mle_org_id BIGINT := (SELECT id FROM organizations WHERE acronym = 'MLE');
    BEGIN
        INSERT INTO data_sources (name, org_id, destination_table, url, data_format, comment)
        VALUES
            ('MLE Leagues', mle_org_id, 'skill_groups','https://sprocket-public-datasets.nyc3.cdn.digitaloceanspaces.com/datasets/leagues.parquet','parquet', NULL),
            ('MLE Teams', mle_org_id, 'teams','https://sprocket-public-datasets.nyc3.cdn.digitaloceanspaces.com/datasets/teams.parquet','parquet', NULL),
            ('MLE Players', mle_org_id, 'players', 'https://sprocket-public-datasets.nyc3.cdn.digitaloceanspaces.com/datasets/players.parquet', 'parquet', NULL),
            ('MLE Members', mle_org_id, 'players', 'https://sprocket-public-datasets.nyc3.cdn.digitaloceanspaces.com/datasets/members.parquet', 'parquet', 'used to find players by discord_id and not added to the player table');
    END $$;