/*
    TABLE: data_sources
    CHANGES: add last_imported_at to track when each data source was last imported successfully
 */

ALTER TABLE data_sources
    ADD last_imported_at TIMESTAMP WITH TIME ZONE;
