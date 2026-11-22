-- Owned by ingest-static (the only service that runs Flyway). Vector tables themselves are created at runtime by
-- VectorStoreProvider through Spring AI's DDL, one per embedding model. See docs/architecture.md.

create extension if not exists vector;
create extension if not exists hstore;
create extension if not exists "uuid-ossp";

create table index_registry (
    table_name      text primary key,
    embedding_model text        not null,
    dimensions      int         not null,
    active          boolean     not null default false,
    created_at      timestamptz not null default now(),
    activated_at    timestamptz
);

-- At most one active index.
create unique index index_registry_one_active on index_registry (active) where active;

-- Starting point for Story 1 Task 4. The Story 1 owner evolves it with new migrations (V2__..., never edit V1).
create table ingestion_manifest (
    doc_id          text        not null,
    version         text        not null,
    source          text        not null,
    checksum        text        not null,
    embedding_model text        not null,
    table_name      text        not null references index_registry (table_name),
    chunk_count     int         not null,
    ingested_at     timestamptz not null default now(),
    primary key (doc_id, version, table_name)
);
