package com.costin.travelify.dto.tripadvisor_dto;

import lombok.Data;

import java.util.List;

@Data
public class ImagesSearchDTO {
    private List<ImageBundleDTO> data;
}
