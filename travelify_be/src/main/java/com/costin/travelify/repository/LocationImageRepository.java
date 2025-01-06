package com.costin.travelify.repository;

import com.costin.travelify.entities.LocationImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationImageRepository extends JpaRepository<LocationImage, Integer> {
}
