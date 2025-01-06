package com.costin.travelify.dto.request_dto;

import lombok.Data;

@Data
public class UpdateReviewDTO {
    private String title;
    private String content;
    private Integer rating;
}
