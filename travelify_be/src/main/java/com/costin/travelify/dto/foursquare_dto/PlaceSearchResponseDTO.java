package com.costin.travelify.dto.foursquare_dto;

import lombok.Data;

import java.util.List;

@Data
public class PlaceSearchResponseDTO {
    private List<PlaceSearchResultDTO> results;
}
