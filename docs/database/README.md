# EVFinder Database Design

This design keeps the ten relations from the conceptual model and adds the constraints and attributes required by the API contract and data-import workflow.

The executable schema is stored at `docker/mysql/init/001-schema.sql`. Future ERD PDFs and database-design notes belong in this directory, while executable SQL remains with the Docker database configuration.

## Entity Relationship Diagram

```mermaid
erDiagram
    USERS ||--o{ SAVED_VEHICLES : saves
    VEHICLES ||--o{ SAVED_VEHICLES : appears_in
    USERS ||--o{ FAVORITES : marks
    CHARGING_STATIONS ||--o{ FAVORITES : receives
    USERS ||--o{ REVIEWS : writes
    CHARGING_STATIONS ||--o{ REVIEWS : receives
    CHARGING_NETWORKS ||--o{ CHARGING_STATIONS : operates
    VEHICLES ||--o{ VEHICLE_CONNECTORS : supports
    CONNECTORS ||--o{ VEHICLE_CONNECTORS : fits
    CHARGING_STATIONS ||--o{ STATION_CONNECTORS : provides
    CONNECTORS ||--o{ STATION_CONNECTORS : identifies

    USERS {
        BIGINT user_id PK
        VARCHAR email UK
        VARCHAR password_hash
        VARCHAR display_name
    }

    VEHICLES {
        BIGINT vehicle_id PK
        VARCHAR make_name
        VARCHAR model_name
        SMALLINT model_year
        VARCHAR trim_name
        VARCHAR variant_name
        VARCHAR vehicle_type
        VARCHAR drivetrain
        DECIMAL battery_capacity_net_kwh
        DECIMAL ac_max_power_kw
        DECIMAL dc_max_power_kw
        INT range_wltp_km
        INT range_epa_km
    }

    CHARGING_NETWORKS {
        BIGINT network_id PK
        VARCHAR network_name UK
        VARCHAR network_url
    }

    CHARGING_STATIONS {
        BIGINT station_id PK
        VARCHAR external_id UK
        BIGINT network_id FK
        VARCHAR station_name
        VARCHAR street_address
        VARCHAR city
        CHAR state
        VARCHAR zip_code
        DECIMAL latitude
        DECIMAL longitude
        VARCHAR status_code
        VARCHAR access_code
        VARCHAR access_days_time
        VARCHAR facility_type
        INT level1_evse_count
        INT level2_evse_count
        INT dc_fast_count
        VARCHAR pricing_text
    }

    CONNECTORS {
        BIGINT connector_id PK
        VARCHAR connector_name UK
        VARCHAR afdc_code UK
        VARCHAR openev_code UK
    }

    REVIEWS {
        BIGINT review_id PK
        BIGINT user_id FK
        BIGINT station_id FK
        TINYINT rating
        VARCHAR comment
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    SAVED_VEHICLES {
        BIGINT user_id PK,FK
        BIGINT vehicle_id PK,FK
        BOOLEAN is_primary
    }

    FAVORITES {
        BIGINT user_id PK,FK
        BIGINT station_id PK,FK
    }

    VEHICLE_CONNECTORS {
        BIGINT vehicle_id PK,FK
        BIGINT connector_id PK,FK
    }

    STATION_CONNECTORS {
        BIGINT station_id PK,FK
        BIGINT connector_id PK,FK
        INT connector_count
        DECIMAL max_power_kw
    }
```

## Implementation Decisions

- Table names are plural and use `snake_case`.
- `charging_stations.external_id` supports repeatable imports from an external data source.
- `station_connectors` stores connector count and maximum power because those values belong to a station-connector pair.
- Connector count and power are nullable because the AFDC CSV does not always provide those values per connector type. `NULL` means unknown; zero means none.
- `reviews` has a unique constraint on `(user_id, station_id)`.
- `saved_vehicles` uses a generated nullable key so that each user can have at most one primary vehicle.
- MySQL does not allow the generated-column design to be combined with cascading foreign-key actions on this table, so saved-vehicle rows must be removed explicitly before deleting their user or vehicle.
- Search and review-list indexes reflect the current API contract. Indexes will be evaluated with `EXPLAIN` after realistic data is imported.
- Vehicles, favorites, and saved vehicles remain part of the complete design but do not have to be exposed in the first MVP API.
