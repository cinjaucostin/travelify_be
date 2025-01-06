package com.costin.travelify.dto.response_dto;

import lombok.Data;

import java.util.List;

@Data
public class TripsWithCriteriaDTO {
    private String criteria;
    private List<TripMinDetailsDTO> trips;
}
