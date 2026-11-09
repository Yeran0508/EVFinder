# Station Search Query Plan

Dataset: 2,079 AFDC charging stations and 2,396 station-connector associations.

## City Search

```sql
EXPLAIN ANALYZE
SELECT *
FROM charging_stations
WHERE city = 'Chicago'
  AND station_id > 0
ORDER BY station_id
LIMIT 21;
```

MySQL uses a primary-key range scan to preserve cursor order and stop as soon as it has 21 matching rows. On the current local dataset, the query returned 21 rows in about 1.2 ms.

## City and Connector Search

```sql
EXPLAIN ANALYZE
SELECT s.*
FROM charging_stations s
JOIN station_connectors sc ON sc.station_id = s.station_id
JOIN connectors c ON c.connector_id = sc.connector_id
WHERE s.city = 'Chicago'
  AND c.connector_name = 'CCS'
  AND s.station_id > 0
ORDER BY s.station_id
LIMIT 21;
```

The original query used `DISTINCT`, which created a temporary table for deduplication. It was unnecessary because `station_connectors` has the primary key `(station_id, connector_id)`, so a station can match a connector only once. Removing `DISTINCT` eliminated the temporary table. The measured execution time changed from about 4.1 ms to 1.8 ms on the current local dataset.

These measurements are local observations, not production guarantees. Query plans must be checked again after the dataset or query patterns grow materially.

## Rating Sort

Rating search left-joins `reviews`, groups by station, and orders by `AVG(rating)` plus `station_id`. The station ID is the deterministic tie-breaker used by cursor pagination. Unrated stations use an internal sentinel only for ordering and remain `null` in the API response.

On the current dataset, MySQL used `idx_charging_stations_city_id` to select 365 Chicago stations before joining reviews and aggregating. `EXPLAIN ANALYZE` measured about 0.39 ms locally. A temporary aggregate and sort is expected because the rating is calculated from review rows at query time.
