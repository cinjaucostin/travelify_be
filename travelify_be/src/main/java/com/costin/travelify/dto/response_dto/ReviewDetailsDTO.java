package com.costin.travelify.dto.response_dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewDetailsDTO {
    private int id;
    private String title;
    private String content;
    private double rating;
    private LocalDateTime createdDate;
    private int userId;
    private String userTimestamp;
    private String userName;
    private String createdTimestamp;
    private int destinationId;
    private int locationId;
    private int tripId;
    private int reviewedEntityId;
    private String reviewedEntityName;
    private String reviewedEntityType;
}
