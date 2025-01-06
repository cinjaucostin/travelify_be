package com.costin.travelify.dto.request_dto;

import lombok.Data;

@Data
public class AddAppreciationDTO {
    private String type;

    private Integer tripId;
    private Integer destinationId;
    private Integer locationId;
}
