package com.costin.travelify.dto.tomtom_dto;

import lombok.Data;

@Data
public class LocationsSearchResultTTDTO {
    private String id;
    private double score;
    private double dist;
    private PointOfInterestTTDTO poi;
}
