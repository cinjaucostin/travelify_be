package com.costin.travelify.dto.response_dto;

import com.costin.travelify.entities.Review;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class MultipleReviewsDTO {
    private List<ReviewDetailsDTO> reviews;
    private int numberOfPages;
    private int pageNumber;
    private int pageSize;
    private int numberOfElements;
}
