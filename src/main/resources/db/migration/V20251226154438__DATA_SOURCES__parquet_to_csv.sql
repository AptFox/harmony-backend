/*
    TABLE: data_sources
    CHANGES: update data_sources from parquet to csv
 */
DO $$
    DECLARE
        mle_players_data_source_id BIGINT := (SELECT id FROM data_sources WHERE name = 'MLE Players');
    DECLARE
        mle_leagues_data_source_id BIGINT := (SELECT id FROM data_sources WHERE name = 'MLE Leagues');
    DECLARE
        mle_teams_data_source_id BIGINT := (SELECT id FROM data_sources WHERE name = 'MLE Teams');
    DECLARE
        mle_members_data_source_id BIGINT := (SELECT id FROM data_sources WHERE name = 'MLE Members');
    BEGIN
        -- Updates to players data source
        UPDATE data_sources SET url = 'https://sprocket-public-datasets.nyc3.cdn.digitaloceanspaces.com/datasets/players.csv' WHERE id = mle_players_data_source_id;
        UPDATE data_sources SET data_format = 'csv' WHERE id = mle_players_data_source_id;
        -- Updates to leagues data source
        UPDATE data_sources SET url = 'https://sprocket-public-datasets.nyc3.cdn.digitaloceanspaces.com/datasets/leagues.csv' WHERE id = mle_leagues_data_source_id;
        UPDATE data_sources SET data_format = 'csv' WHERE id = mle_leagues_data_source_id;
        -- Updates to teams data source
        UPDATE data_sources SET url = 'https://sprocket-public-datasets.nyc3.cdn.digitaloceanspaces.com/datasets/teams.csv' WHERE id = mle_teams_data_source_id;
        UPDATE data_sources SET data_format = 'csv' WHERE id = mle_teams_data_source_id;
        -- Updates to members data source
        UPDATE data_sources SET destination_table = 'users' WHERE id = mle_members_data_source_id;
        UPDATE data_sources SET url = 'https://sprocket-public-datasets.nyc3.cdn.digitaloceanspaces.com/datasets/members.csv' WHERE id = mle_members_data_source_id;
        UPDATE data_sources SET data_format = 'csv' WHERE id = mle_members_data_source_id;
        UPDATE data_sources SET comment = NULL WHERE id = mle_members_data_source_id;
    END $$;