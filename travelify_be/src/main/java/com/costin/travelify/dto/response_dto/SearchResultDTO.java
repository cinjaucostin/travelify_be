package com.costin.travelify.dto.response_dto;

import com.costin.travelify.entities.Destination;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResultDTO {
    private int id;
    private long tripadvisorId;
    private String name;
    private String address;
    private double rating;
    private int reviews;
    private double popularity;
    private String categories;
    private String type;
    private String resultType;
    private String image;
    private double matchScore;
}
