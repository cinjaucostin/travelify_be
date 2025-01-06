package com.costin.travelify.repository;

import com.costin.travelify.entities.TripPlannificationDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripPlannificationDayRepository extends JpaRepository<TripPlannificationDay, Integer> {
}
