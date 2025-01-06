package com.costin.travelify.dto.response_dto;

import jakarta.persistence.Column;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AddFeedbackDTO {
    private Double rating;
    private String content;
}
