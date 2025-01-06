package com.costin.travelify.dto.response_dto;

import com.costin.travelify.entities.LocationImage;
import lombok.Data;

import java.util.List;

@Data
public class ImagesDTO {
    private List<LocationImage> images;
}
