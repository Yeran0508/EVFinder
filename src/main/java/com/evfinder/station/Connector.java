package com.evfinder.station;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "connectors")
public class Connector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "connector_id")
    private Long connectorId;

    @Column(name = "connector_name", nullable = false, unique = true, length = 100)
    private String connectorName;

    protected Connector() {
    }

    public Long getConnectorId() {
        return connectorId;
    }

    public String getConnectorName() {
        return connectorName;
    }
}
