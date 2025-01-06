package com.costin.travelify.dto.response_dto;

import com.costin.travelify.entities.Destination;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OneDestinationDTO {
    private Destination destination;
}
