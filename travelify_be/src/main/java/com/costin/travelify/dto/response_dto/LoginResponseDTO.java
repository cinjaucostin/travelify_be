package com.costin.travelify.dto.response_dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private int userId;
    private String token;
    private String profileImage;
}
