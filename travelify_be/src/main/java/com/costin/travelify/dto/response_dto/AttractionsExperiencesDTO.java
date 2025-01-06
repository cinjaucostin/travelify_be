package com.costin.travelify.dto.response_dto;

import lombok.Data;

import java.util.List;

@Data
public class AttractionsExperiencesDTO {
    private List<ExperienceWithLocationsDTO> experiences;
}
