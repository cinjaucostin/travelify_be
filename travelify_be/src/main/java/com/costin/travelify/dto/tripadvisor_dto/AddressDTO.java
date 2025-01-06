package com.costin.travelify.dto.tripadvisor_dto;

import lombok.Data;

@Data
public class AddressDTO {
    private String city;
    private String country;
    private String address_string;
}
