package com.costin.travelify.dto.tripadvisor_dto;

import lombok.Data;

import java.util.List;

@Data
public class GroupDTO {
    private String name;
    private String localized_name;
    private List<CategoryDTO> categories;
}
