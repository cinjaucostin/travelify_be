package com.costin.travelify.dto.tripadvisor_dto;

import lombok.Data;

import java.util.List;

@Data
public class LocationSearchDTO {
    private List<LocationSearchResultDTO> data;
}
