package com.mini_project.redis_serialize.presentation.dto;

import lombok.Builder;

@Builder
public record ResponseDto(String serializationType,
                          int threadCount,
                          int count,
                          long saveElapsedMillis,
                          long readElapsedMillis,
                          long totalMemoryBytes) {
}
