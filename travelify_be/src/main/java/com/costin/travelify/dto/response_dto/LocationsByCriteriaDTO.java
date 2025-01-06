package com.costin.travelify.dto.response_dto;

import com.costin.travelify.entities.Location;
import lombok.Data;

import java.util.List;

@Data
public class LocationsByCriteriaDTO {
    private String criteria;
    private List<Location> locations;
}
