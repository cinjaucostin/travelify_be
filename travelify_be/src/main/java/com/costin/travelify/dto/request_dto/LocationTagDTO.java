package com.costin.travelify.dto.request_dto;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
public class LocationTagDTO {
    private int id;
    private String tag;
    private String name;
}
