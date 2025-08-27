package com.owmetagg.dtos;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SessionDTO {
    private String sessionId;
    private LocalDateTime sessionStart;
    private LocalDateTime sessionEnd;
    private Integer duration; // in minutes
    private String platform;
}