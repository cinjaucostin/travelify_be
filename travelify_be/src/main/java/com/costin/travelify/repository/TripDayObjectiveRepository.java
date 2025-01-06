package com.costin.travelify.repository;

import com.costin.travelify.entities.TripDayObjective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripDayObjectiveRepository extends JpaRepository<TripDayObjective, Integer> {
}
