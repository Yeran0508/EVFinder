# EVFinder Data Source Plan

## Purpose

EVFinder uses two different kinds of data during development:

1. Small deterministic fixtures for automated tests.
2. Real public charging-station data for import development, product validation, and performance testing.

Automated tests must not depend on a live third-party API. A small, reviewed fixture should remain stable so the same test produces the same result every time.

## Planned Real Data Source

The primary candidate is the U.S. Department of Energy Alternative Fuels Data Center (AFDC) station dataset or API. Before importing data, verify:

- usage terms and attribution requirements;
- authentication or API-key requirements;
- update frequency;
- station and connector field definitions;
- missing-value behavior;
- stable external station identifiers;
- pagination and rate limits.

Additional sources such as Open Charge Map should only be combined after checking their licenses, identifiers, and duplicate-station behavior.

## Repository Layout

```text
docs/database/                     Database design and ERD documentation
docker/mysql/init/                 Executable schema used for a new local database
data/raw/                          Large downloaded source files; local only
data/processed/                    Local generated import output; local only
src/test/resources/fixtures/       Small reviewed test datasets committed to Git
```

## Import Stages

1. Preserve the original external identifier in `charging_stations.external_id`.
2. Map source fields to the EVFinder schema without silently inventing missing values.
3. Normalize connector types through the `connectors` table.
4. Make repeated imports idempotent so the same station is updated instead of duplicated.
5. Record rejected or malformed rows for inspection.
6. Validate row counts, null rates, duplicate identifiers, coordinates, and connector mappings.
7. Use a larger snapshot only for query-plan and index testing.

## Testing Rule

- Unit and integration tests use small local fixtures.
- Import tests use a fixed source sample.
- Performance tests use a larger reproducible snapshot.
- Manual product exploration may use the latest real dataset.
- A live external API is never required for the normal Maven test command.
