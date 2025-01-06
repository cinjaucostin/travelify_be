package com.costin.travelify.dto.arcgis_dto;

import lombok.Data;

import java.util.List;

@Data
public class RoutesDTO {
    private List<FeatureDTO> features;
}
