package com.costin.travelify.dto.response_dto;

import lombok.Data;

@Data
public class TripPopularityDTO {
    private double popularityScore;
    private double tripReviewsScore;
}
