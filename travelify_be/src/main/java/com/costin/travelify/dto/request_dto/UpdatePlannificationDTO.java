package com.costin.travelify.dto.request_dto;

import lombok.Data;
@Data
public class UpdatePlannificationDTO {
    private Integer sourcePlanDayId;
    private Integer destinationPlanDayId;
    private Integer movedObjectiveId;
    private Integer afterObjectiveId;
}
