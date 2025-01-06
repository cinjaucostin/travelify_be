package com.costin.travelify.dto.tripadvisor_dto;

import lombok.Data;

@Data
public class RankingDTO {
    private String ranking_string;
    private int ranking_out_of;
    private int ranking;
}
