package com.costin.travelify.dto.request_dto;

import lombok.Data;

import java.util.List;

@Data
public class GenerateTripDTO {
    private Integer destinationId;
    private List<LocationTagDTO> tripTags;
    private String periodType;
    private String firstDay;
    private String lastDay;
    private Integer numberOfDays;
    private Integer maxNrOfObjectivesPerDay;
    private String month;
}
