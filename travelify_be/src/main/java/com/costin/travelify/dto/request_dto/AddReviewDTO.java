package com.costin.travelify.dto.request_dto;

import lombok.Data;

@Data
public class AddReviewDTO {
    private String title;
    private String content;
    private Double rating;
    private String type;

    private Integer tripId;
    private Integer destinationId;
    private Integer locationId;
}
