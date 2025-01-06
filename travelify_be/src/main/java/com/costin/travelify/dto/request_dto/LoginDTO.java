package com.costin.travelify.dto.request_dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String email;
    private String password;
}