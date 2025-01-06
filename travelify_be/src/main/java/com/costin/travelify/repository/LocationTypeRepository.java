package com.costin.travelify.repository;

import com.costin.travelify.entities.LocationTag;
import com.costin.travelify.entities.LocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocationTypeRepository extends JpaRepository<LocationType, Integer> {
    Optional<LocationType> findByType(String type);
}
