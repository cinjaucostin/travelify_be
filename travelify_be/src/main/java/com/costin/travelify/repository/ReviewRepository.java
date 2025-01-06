package com.costin.travelify.repository;

import com.costin.travelify.entities.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    @Query("""
            select r from Review r inner join User u on r.user.id=u.id
            where u.id = :userId
            """)
    List<Review> findReviewsByUserId(int userId);

    @Query("""
            select r from Review r inner join User u on r.user.id=u.id
            where u.id = :userId
            """)
    Page<Review> findReviewsByUserIdPaged(int userId, Pageable pageable);

    @Query("""
            select r from Review r inner join r.location l
            where l.id = :locationId
            """)
    Page<Review> findReviewsByLocationId(int locationId, Pageable pageable);

    @Query("""
            select r from Review r inner join r.destination d
            where d.id = :destinationId
            """)
    Page<Review> findReviewsByDestinationId(int destinationId, Pageable pageable);

    @Query("""
            select r from Review r inner join r.trip t
            where t.id = :tripId
            """)
    Page<Review> findReviewsByTripId(int tripId, Pageable pageable);


}
