package com.costin.travelify.dto.response_dto;

import com.costin.travelify.entities.Review;
import com.costin.travelify.service.ReviewService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OneReviewDTO {
    private Review review;
}
