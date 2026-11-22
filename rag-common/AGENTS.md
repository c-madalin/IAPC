# rag-common (infra)

Plain Java, no dependencies. The contracts between services (`api/*` records), `MetadataKeys`, `ServiceNames` and
`MetricNames`. A change here affects every service: update `docs/contracts.md` and every caller in the same PR, add
fields rather than renaming them, and ask the team before removing anything.
