package com.costin.travelify.dto.tripadvisor_dto;

import com.costin.travelify.entities.Destination;
import lombok.Data;

import java.util.List;

@Data
public class LocationDetailsDTO {
    private long location_id;
    private String name;
    private String description;
    private AddressDTO address_obj;
    private double latitude;
    private double longitude;
    private String timezone;
    private CategoryDTO category;
    private List<CategoryDTO> subcategory;
    private RankingDTO ranking_data;
    private double rating;
    private int num_reviews;
    private List<GroupDTO> groups;
    private List<String> styles;
    private List<String> amenities;
    private List<String> features;
    private List<CategoryDTO> cuisine;
    private String price_level;
    private WorkHoursDTO hours;
    private List<AncestorLocationDTO> ancestors;
    private List<LocationSearchResultDTO> neighborhood_info;
}
