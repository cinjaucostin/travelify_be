package com.costin.travelify.dto.response_dto;

import com.costin.travelify.entities.Review;
import lombok.Data;

import java.util.List;

@Data
public class RatingDTO {
    private String type;
    private Integer id;
    private double rating;
    private List<Review> travelifyReviews;
    private Integer locationId;
    private Integer destinationId;
    private Integer tripId;
}
