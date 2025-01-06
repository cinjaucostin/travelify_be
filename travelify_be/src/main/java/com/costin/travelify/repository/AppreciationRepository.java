package com.costin.travelify.repository;

import com.costin.travelify.entities.Appreciation;
import com.costin.travelify.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppreciationRepository extends JpaRepository<Appreciation, Integer> {
    @Query("""
            select a from Appreciation a inner join Location l on a.location.id=l.id
            where l.id = :locationId
            """)
    List<Appreciation> findAppreciationsByLocationId(int locationId);

    @Query("""
            select a from Appreciation a inner join Destination d on a.destination.id=d.id
            where d.id = :destinationId
            """)
    List<Appreciation> findAppreciationsByDestinationId(int destinationId);

    @Query("""
            select a from Appreciation a inner join Trip t on a.trip.id=t.id
            where t.id = :tripId
            """)
    List<Appreciation> findAppreciationsByTripId(int tripId);

    @Query("""
            select a from Appreciation a inner join User u on a.user.id=u.id
            where u.id = :userId
            """)
    List<Appreciation> findAppreciationsByUserId(int userId);
}
