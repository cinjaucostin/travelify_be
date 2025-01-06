package com.costin.travelify.service;

import com.costin.travelify.dto.tripadvisor_dto.CategoryDTO;
import com.costin.travelify.entities.LocationType;
import com.costin.travelify.repository.LocationTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LocationTypeService {
    @Autowired
    private LocationTypeRepository locationTypeRepository;

    public LocationType saveLocationTypeOrGetExisting(CategoryDTO categoryDTO) {

        Optional<LocationType> existingLocationTypeOptional = this.findByType(categoryDTO.getName());

        if(existingLocationTypeOptional.isPresent()) {
            return existingLocationTypeOptional.get();
        }

        LocationType newLocationType = new LocationType();
        newLocationType.setType(categoryDTO.getName());
        newLocationType.setName(categoryDTO.getLocalized_name());

        return this.saveLocationType(newLocationType);
    }

    public LocationType saveLocationType(LocationType locationType) {
        return this.locationTypeRepository.save(locationType);
    }

    public Optional<LocationType> findByType(String type) {
        return this.locationTypeRepository.findByType(type);
    }

}
