package com.mini_project.redis_serialize.presentation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

public record BatchSaveRequest(String serializationType,
                               @Positive @Max(message = "6이하 양의 정수 가능", value = 6) int threadCount,
                               @Positive @Max(message = "2000이하 양의 정수 가능", value = 2000) int count) {
}
