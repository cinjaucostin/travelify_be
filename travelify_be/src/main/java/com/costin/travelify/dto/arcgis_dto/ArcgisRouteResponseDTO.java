package com.costin.travelify.dto.arcgis_dto;

import lombok.Data;

@Data
public class ArcgisRouteResponseDTO {
    private String checksum;
    private String requestID;
    private RoutesDTO routes;
}
