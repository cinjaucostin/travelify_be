package com.costin.travelify.dto.response_dto;

import lombok.Data;

import java.util.List;

@Data
public class MultipleFeedbacksDTO {
    private List<FeedbackDetailsDTO> feedbacks;
    private int numberOfPages;
    private int pageNumber;
    private int pageSize;
    private int numberOfElements;
}
