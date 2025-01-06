package com.costin.travelify.repository;

import com.costin.travelify.entities.Destination;
import com.costin.travelify.entities.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Integer> {
    @Query("SELECT l " +
            "FROM Location l " +
            "JOIN l.locationType lt " +
            "WHERE (l.description LIKE %:location% " +
            "OR l.addressPath LIKE %:location% " +
            "OR l.name LIKE %:location%) " +
            "AND lt.type = :locationType")
    List<Location> findByNameOrDescriptionOrAddressPathContainingFilterByLocationType(String location, String locationType);

    @Query("SELECT l " +
            "FROM Location l " +
            "JOIN l.locationType lt " +
            "WHERE l.description LIKE %:location% " +
            "OR l.addressPath LIKE %:location% " +
            "OR l.name LIKE %:location%")
    List<Location> findByNameOrDescriptionOrAddressPathContaining(String location);
    Optional<Location> findByTripadvisorId(long tripadvisorId);
}
