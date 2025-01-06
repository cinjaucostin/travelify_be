package com.costin.travelify.repository;

import com.costin.travelify.entities.Location;
import com.costin.travelify.entities.Review;
import com.costin.travelify.entities.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Integer> {
    @Query("SELECT t " +
            "FROM Trip t " +
            "JOIN t.destination d " +
            "WHERE t.name LIKE %:query% " +
            "OR d.name LIKE %:query% " +
            "OR d.addressPath LIKE %:query%")
    List<Trip> findByNameOrDestinationContainsQuery(String query);

    @Query("SELECT t " +
            "FROM Trip t " +
            "ORDER BY t.createdTimestamp DESC")
    List<Trip> findLatestTrips();

    @Query("""
            select t from Trip t inner join t.user u
            where u.id = :userId
            order by t.createdTimestamp desc
            """)
    Page<Trip> findTripsByUserId(int userId, Pageable pageable);

    @Query("""
            select t from Trip t inner join t.destination d
            where d.id = :destinationId
            """)
    Page<Trip> findTripsByDestinationId(int destinationId, Pageable pageable);

}
