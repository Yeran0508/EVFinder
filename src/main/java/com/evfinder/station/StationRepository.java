package com.evfinder.station;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, Long> {

    List<Station> findByCityOrderByStationIdAsc(String city);
}
