package com.owmetagg.dtos.overfast;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OverFastPlayerStatsDTO {
    @JsonProperty("username")
    private String username;

    @JsonProperty("avatar")
    private String avatar;

    @JsonProperty("namecard")
    private String namecard;

    @JsonProperty("title")
    private String title;

    @JsonProperty("endorsement")
    private OverFastEndorsementDTO endorsement;

    @JsonProperty("competitive")
    private Map<String, OverFastCompetitiveDTO> competitive;

    @JsonProperty("stats")
    private OverFastStatsDTO stats;
}