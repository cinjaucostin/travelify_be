package com.costin.travelify.dto.response_dto;

import com.costin.travelify.entities.Review;
import lombok.Data;

import java.util.List;

@Data
public class PagedTripsDetailsDTO {
    private List<TripMinDetailsDTO> trips;
    private int numberOfPages;
    private int pageNumber;
    private int pageSize;
    private int numberOfElements;
}
