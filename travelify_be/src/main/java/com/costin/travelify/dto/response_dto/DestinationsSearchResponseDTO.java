package com.costin.travelify.dto.response_dto;

import com.costin.travelify.controller.DestinationController;
import com.costin.travelify.entities.Destination;
import lombok.Data;

import java.util.List;

@Data
public class DestinationsSearchResponseDTO {
    private List<SearchResultDTO> destinations;
}
