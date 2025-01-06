package com.costin.travelify.dto.response_dto;

import lombok.Data;

@Data
public class LocationPopularityDTO {
    private double popularityScore;
    private double locationsReviewsScore;
    private double locationsViewsScore;
    private double tripsScore;
    private String ranking;
}
