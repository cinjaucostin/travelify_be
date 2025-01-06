package com.costin.travelify.service;

import com.costin.travelify.dto.request_dto.LocationTagDTO;
import com.costin.travelify.dto.tripadvisor_dto.CategoryDTO;
import com.costin.travelify.dto.tripadvisor_dto.GroupDTO;
import com.costin.travelify.entities.LocationTag;
import com.costin.travelify.entities.LocationType;
import com.costin.travelify.repository.LocationTagRepository;
import com.costin.travelify.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LocationTagService {
    @Autowired
    private LocationTagRepository locationTagRepository;

    public String extractNamesFromListOfLocationTags(List<LocationTag> locationTags) {
        if(locationTags == null) {
            return "";
        }

        if(locationTags.isEmpty()) {
            return "";
        }

        Set<LocationTag> locationTagSet = new HashSet<>(locationTags);
        return Utils.joinListOfStrings(
                locationTagSet.stream().map(LocationTag::getName).collect(Collectors.toList()),
                ", "
        );
    }

    public List<LocationTag> getAssociatedLocationTagsWithGivenDTO(List<LocationTagDTO> locationTagDTOS) {
        List<LocationTag> locationsTags = new ArrayList<>();
        locationTagDTOS.forEach(locationTagDTO -> {
            Optional<LocationTag> locationTagOptional = this.locationTagRepository.findById(locationTagDTO.getId());
            locationTagOptional.ifPresent(locationsTags::add);
        });
        return locationsTags;
    }

    public LocationTag saveLocationTagOrGetExistingFromCategory(CategoryDTO categoryDTO) {
        Optional<LocationTag> existingLocationTagOptional = this.findByType(categoryDTO.getName());

        if(existingLocationTagOptional.isPresent()) {
            return existingLocationTagOptional.get();
        }

        LocationTag newLocationTag = new LocationTag();
        newLocationTag.setTag(categoryDTO.getName());
        newLocationTag.setName(categoryDTO.getLocalized_name());

        return this.saveLocationType(newLocationTag);
    }

    public List<LocationTag> saveLocationsTagsOrGetExistingFromGroup(GroupDTO groupDTO) {
        List<CategoryDTO> categoryDTOS = groupDTO.getCategories();
        List<LocationTag> locationTags = new ArrayList<>();

        categoryDTOS.forEach(categoryDTO -> {
            locationTags.add(this.saveLocationTagOrGetExistingFromCategory(categoryDTO));
        });

        return locationTags;
    }

    public LocationTag saveLocationType(LocationTag locationTag) {
        return this.locationTagRepository.save(locationTag);
    }

    public Optional<LocationTag> findByType(String type) {
        return this.locationTagRepository.findByTag(type);
    }

}
