package com.costin.travelify.dto.response_dto;

import lombok.Data;

@Data
public class DestinationPopularityDTO {
    private double popularityScore;
    private double destinationReviewsScore;
    private double destinationViewsScore;
    private double locationsReviewsScore;
    private double locationsViewsScore;
    private double tripsScore;
}
