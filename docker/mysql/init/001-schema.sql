CREATE TABLE users (
    user_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    external_id VARCHAR(64) NOT NULL,
    email VARCHAR(254),
    password_hash VARCHAR(255),
    display_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_external_id (external_id),
    UNIQUE KEY uk_users_email (email)
);

CREATE TABLE vehicles (
    vehicle_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    make_name VARCHAR(100) NOT NULL,
    model_name VARCHAR(150) NOT NULL,
    model_year SMALLINT UNSIGNED NOT NULL,
    trim_name VARCHAR(150),
    variant_name VARCHAR(150),
    vehicle_type VARCHAR(50),
    drivetrain VARCHAR(50),
    battery_capacity_net_kwh DECIMAL(8, 2),
    ac_max_power_kw DECIMAL(8, 2),
    dc_max_power_kw DECIMAL(8, 2),
    range_wltp_km INT UNSIGNED,
    range_epa_km INT UNSIGNED,
    PRIMARY KEY (vehicle_id),
    CONSTRAINT chk_vehicles_model_year CHECK (model_year BETWEEN 1990 AND 2200),
    CONSTRAINT chk_vehicles_battery CHECK (battery_capacity_net_kwh IS NULL OR battery_capacity_net_kwh >= 0),
    CONSTRAINT chk_vehicles_ac_power CHECK (ac_max_power_kw IS NULL OR ac_max_power_kw >= 0),
    CONSTRAINT chk_vehicles_dc_power CHECK (dc_max_power_kw IS NULL OR dc_max_power_kw >= 0)
);

CREATE TABLE charging_networks (
    network_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    network_name VARCHAR(150) NOT NULL,
    network_url VARCHAR(2048),
    PRIMARY KEY (network_id),
    UNIQUE KEY uk_charging_networks_name (network_name)
);

CREATE TABLE charging_stations (
    station_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    external_id VARCHAR(64) NOT NULL,
    network_id BIGINT UNSIGNED NOT NULL,
    station_name VARCHAR(255) NOT NULL,
    street_address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state CHAR(2) NOT NULL,
    zip_code VARCHAR(10),
    latitude DECIMAL(9, 6) NOT NULL,
    longitude DECIMAL(9, 6) NOT NULL,
    status_code VARCHAR(20),
    access_code VARCHAR(20),
    access_days_time VARCHAR(255),
    facility_type VARCHAR(100),
    level1_evse_count INT UNSIGNED NOT NULL DEFAULT 0,
    level2_evse_count INT UNSIGNED NOT NULL DEFAULT 0,
    dc_fast_count INT UNSIGNED NOT NULL DEFAULT 0,
    pricing_text VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (station_id),
    UNIQUE KEY uk_charging_stations_external_id (external_id),
    KEY idx_charging_stations_city_id (city, station_id),
    KEY idx_charging_stations_location (latitude, longitude),
    CONSTRAINT fk_charging_stations_network
        FOREIGN KEY (network_id) REFERENCES charging_networks (network_id),
    CONSTRAINT chk_charging_stations_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_charging_stations_longitude CHECK (longitude BETWEEN -180 AND 180)
);

CREATE TABLE connectors (
    connector_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    connector_name VARCHAR(100) NOT NULL,
    afdc_code VARCHAR(50),
    openev_code VARCHAR(50),
    PRIMARY KEY (connector_id),
    UNIQUE KEY uk_connectors_name (connector_name),
    UNIQUE KEY uk_connectors_afdc_code (afdc_code),
    UNIQUE KEY uk_connectors_openev_code (openev_code)
);

CREATE TABLE reviews (
    review_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    station_id BIGINT UNSIGNED NOT NULL,
    rating TINYINT UNSIGNED NOT NULL,
    comment VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (review_id),
    UNIQUE KEY uk_reviews_user_station (user_id, station_id),
    KEY idx_reviews_station_created (station_id, created_at DESC, review_id DESC),
    CONSTRAINT fk_reviews_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_station
        FOREIGN KEY (station_id) REFERENCES charging_stations (station_id) ON DELETE CASCADE,
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE TABLE saved_vehicles (
    user_id BIGINT UNSIGNED NOT NULL,
    vehicle_id BIGINT UNSIGNED NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    primary_user_id BIGINT UNSIGNED
        GENERATED ALWAYS AS (CASE WHEN is_primary THEN user_id ELSE NULL END) STORED,
    PRIMARY KEY (user_id, vehicle_id),
    UNIQUE KEY uk_saved_vehicles_one_primary (primary_user_id),
    CONSTRAINT fk_saved_vehicles_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_saved_vehicles_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles (vehicle_id)
);

CREATE TABLE favorites (
    user_id BIGINT UNSIGNED NOT NULL,
    station_id BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, station_id),
    CONSTRAINT fk_favorites_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_station
        FOREIGN KEY (station_id) REFERENCES charging_stations (station_id) ON DELETE CASCADE
);

CREATE TABLE vehicle_connectors (
    vehicle_id BIGINT UNSIGNED NOT NULL,
    connector_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (vehicle_id, connector_id),
    KEY idx_vehicle_connectors_connector (connector_id, vehicle_id),
    CONSTRAINT fk_vehicle_connectors_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles (vehicle_id) ON DELETE CASCADE,
    CONSTRAINT fk_vehicle_connectors_connector
        FOREIGN KEY (connector_id) REFERENCES connectors (connector_id)
);

CREATE TABLE station_connectors (
    station_id BIGINT UNSIGNED NOT NULL,
    connector_id BIGINT UNSIGNED NOT NULL,
    connector_count INT UNSIGNED,
    max_power_kw DECIMAL(8, 2),
    PRIMARY KEY (station_id, connector_id),
    KEY idx_station_connectors_connector (connector_id, station_id),
    CONSTRAINT fk_station_connectors_station
        FOREIGN KEY (station_id) REFERENCES charging_stations (station_id) ON DELETE CASCADE,
    CONSTRAINT fk_station_connectors_connector
        FOREIGN KEY (connector_id) REFERENCES connectors (connector_id),
    CONSTRAINT chk_station_connectors_power CHECK (max_power_kw IS NULL OR max_power_kw >= 0)
);
