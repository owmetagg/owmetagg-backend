package com.owmetagg.dtos;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatsResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private StatsMetadata metadata;

    public static <T> StatsResponse<T> success(T data, StatsMetadata metadata) {
        return new StatsResponse<>(true, "Success", data, metadata);
    }

    public static <T> StatsResponse<T> success(T data) {
        return new StatsResponse<>(true, "Success", data, null);
    }

    public static <T> StatsResponse<T> error(String message) {
        return new StatsResponse<>(false, message, null, null);
    }
}