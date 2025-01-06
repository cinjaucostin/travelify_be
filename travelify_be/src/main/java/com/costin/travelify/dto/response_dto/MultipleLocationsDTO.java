package com.costin.travelify.dto.response_dto;

import com.costin.travelify.entities.Location;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MultipleLocationsDTO {
    private List<Location> locations;
}
