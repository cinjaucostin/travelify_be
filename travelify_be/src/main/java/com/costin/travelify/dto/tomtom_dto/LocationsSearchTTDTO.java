package com.costin.travelify.dto.tomtom_dto;

import lombok.Data;

import java.util.List;

@Data
public class LocationsSearchTTDTO {
    private List<LocationsSearchResultTTDTO> results;
}
