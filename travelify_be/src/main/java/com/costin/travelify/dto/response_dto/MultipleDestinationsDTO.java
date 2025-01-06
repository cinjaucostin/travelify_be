package com.costin.travelify.dto.response_dto;

import com.costin.travelify.entities.Destination;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MultipleDestinationsDTO {
    private List<Destination> destinations;
}
