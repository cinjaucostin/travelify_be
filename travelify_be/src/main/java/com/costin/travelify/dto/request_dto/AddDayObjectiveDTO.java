package com.costin.travelify.dto.request_dto;

import lombok.Data;

@Data
public class AddDayObjectiveDTO {
    private Integer tripId;
    private Integer tripPlannificationDayId;
    private Integer locationIdToAdd;
}
