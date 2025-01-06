package com.costin.travelify.dto.response_dto;

import com.costin.travelify.entities.Recommendation;
import lombok.Data;

import java.util.List;

@Data
public class TripRecommendationDTO {
    private String criteria;
    private List<Recommendation> recommendations;
}
