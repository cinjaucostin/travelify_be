package com.costin.travelify.dto.response_dto;

import com.costin.travelify.entities.Appreciation;
import com.costin.travelify.entities.Destination;
import com.costin.travelify.entities.Location;
import lombok.Data;

@Data
public class AppreciationDTO {
    private String type;
    private Appreciation appreciation;
    private DestinationMinDetailsDTO destination;
    private Location location;
    private TripMinDetailsDTO trip;
}
