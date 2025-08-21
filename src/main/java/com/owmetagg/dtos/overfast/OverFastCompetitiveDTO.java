package com.owmetagg.dtos.overfast;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OverFastCompetitiveDTO {
    @JsonProperty("rank")
    private Integer rank;

    @JsonProperty("tier")
    private String tier;

    @JsonProperty("role_icon")
    private String roleIcon;

    @JsonProperty("rank_icon")
    private String rankIcon;
}