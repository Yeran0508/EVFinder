# AFDC Import Mapping

## Station Fields

| AFDC CSV column | EVFinder target | Rule |
| --- | --- | --- |
| `ID` | `charging_stations.external_id` | Required; stored as text and used as the idempotency key |
| `Station Name` | `charging_stations.station_name` | Required; trim surrounding whitespace |
| `Street Address` | `charging_stations.street_address` | Required in the first importer; reject the row if missing |
| `City` | `charging_stations.city` | Required |
| `State` | `charging_stations.state` | Required; two-letter code |
| `ZIP` | `charging_stations.zip_code` | Stored as text to preserve leading zeroes |
| `Latitude` | `charging_stations.latitude` | Required; range -90 to 90 |
| `Longitude` | `charging_stations.longitude` | Required; range -180 to 180 |
| `Status Code` | `charging_stations.status_code` | Optional source code |
| `Access Code` | `charging_stations.access_code` | Optional; currently `public` or `private` |
| `Access Days Time` | `charging_stations.access_days_time` | Optional |
| `Facility Type` | `charging_stations.facility_type` | Optional |
| `EV Level1 EVSE Num` | `charging_stations.level1_evse_count` | Missing becomes zero |
| `EV Level2 EVSE Num` | `charging_stations.level2_evse_count` | Missing becomes zero |
| `EV DC Fast Count` | `charging_stations.dc_fast_count` | Missing becomes zero |
| `EV Pricing` | `charging_stations.pricing_text` | Optional |

## Network Fields

| AFDC CSV column | EVFinder target | Rule |
| --- | --- | --- |
| `EV Network` | `charging_networks.network_name` | Required; upsert by unique name |
| `EV Network Web` | `charging_networks.network_url` | Optional |

## Connector Normalization

`EV Connector Types` contains space-separated AFDC codes. Each code becomes one connector record and one station-connector association.

| AFDC code | EVFinder connector name |
| --- | --- |
| `J1772` | `J1772` |
| `J1772COMBO` | `CCS` |
| `CHADEMO` | `CHAdeMO` |
| `TESLA` | `NACS` |
| `NEMA1450` | `NEMA 14-50` |
| `NEMA515` | `NEMA 5-15` |
| `NEMA520` | `NEMA 5-20` |

Unknown connector codes reject the row instead of being silently discarded.

The CSV does not provide reliable connector-level count or power. The importer therefore stores `NULL` for `station_connectors.connector_count` and `max_power_kw`. Station-level Level 1, Level 2, and DC Fast totals are preserved on `charging_stations`.

## Repeat Import Behavior

- Networks are upserted by `network_name`.
- Connectors are upserted by normalized `connector_name`.
- Stations are upserted by `external_id`.
- Existing station-connector rows are deleted and rebuilt for each imported station.
- Importing the same file twice must not increase the number of stations, networks, connectors, or station-connector pairs.
