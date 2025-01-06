package com.costin.travelify.repository;

import com.costin.travelify.entities.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DestinationRepository extends JpaRepository<Destination, Integer> {
    Optional<Destination> findByTripadvisorId(long tripadvisorId);

    @Query("SELECT d " +
            "FROM Destination d " +
            "WHERE d.description LIKE %:destination% " +
            "OR d.addressPath LIKE %:destination% " +
            "OR d.name LIKE %:destination%")
    List<Destination> findByNameOrDescriptionOrAddressPathContaining(String destination);
}
