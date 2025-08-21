package com.owmetagg.dtos.overfast;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OverFastHeroDetailDTO {
    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("portrait")
    private String portrait;

    @JsonProperty("role")
    private String role;

    @JsonProperty("location")
    private String location;

    @JsonProperty("abilities")
    private List<OverFastAbilityDTO> abilities;

    @JsonProperty("story")
    private OverFastStoryDTO story;
}