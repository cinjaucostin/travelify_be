package com.costin.travelify.dto.response_dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedbackDetailsDTO {
    private int id;
    private String content;
    private double rating;
    private LocalDateTime createdDate;
    private String createdTimestamp;
}
