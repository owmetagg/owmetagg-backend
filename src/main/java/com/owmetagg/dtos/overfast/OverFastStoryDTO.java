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
public class OverFastStoryDTO {
    @JsonProperty("summary")
    private String summary;

    @JsonProperty("media")
    private OverFastMediaDTO media;

    @JsonProperty("chapters")
    private List<OverFastChapterDTO> chapters;
}