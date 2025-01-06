package com.costin.travelify.service;

import com.costin.travelify.dto.tripadvisor_dto.ImageBundleDTO;
import com.costin.travelify.entities.LocationImage;
import com.costin.travelify.repository.LocationImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LocationImageService {
    @Autowired
    private LocationImageRepository locationImageRepository;

    public LocationImage saveLocationImage(LocationImage locationImage) {
        return this.locationImageRepository.save(locationImage);
    }

    public LocationImage createLocationImageFromDTO(ImageBundleDTO imageDTO) {
        LocationImage locationImage = new LocationImage();
        if(imageDTO.getImages() != null) {
            if(imageDTO.getImages().getOriginal() != null) {
                locationImage.setSource(imageDTO.getImages().getOriginal().getUrl());
                locationImage.setTripadvisorId(imageDTO.getId());
                return locationImage;
            }
        }
        return null;
    }

}
