package com.costin.travelify.dto.response_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatusCode;

@Data
@AllArgsConstructor
public class ResponseDTO {
    private String message;
}

