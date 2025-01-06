package com.costin.travelify.dto.response_dto;

import lombok.Data;

import java.util.List;

@Data
public class TripMinDetailsDTO {
    private int tripId;
    private String name;
    private double rating;
    private String addressPath;
    private String periodType;
    private int nrOfDays;
    private String firstDay;
    private String lastDay;
    private String month;
    private String imageSource;
    private String timing;
    private int userId;
    private String username;
    private List<TripDayMinDetailsDTO> tripDays;
}
