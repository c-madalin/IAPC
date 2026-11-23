-- Both ingestion services write to the manifest. Rows from before this migration all came from ingest-static.
alter table ingestion_manifest
    add column source_type text not null default 'static'
        constraint ingestion_manifest_source_type_check check (source_type in ('static', 'dynamic'));
