package com.costin.travelify.dto.response_dto;

import lombok.Data;

import java.util.List;

@Data
public class LocationFactsDTO {
    private List<String> facts;
    private String ranking;
}
