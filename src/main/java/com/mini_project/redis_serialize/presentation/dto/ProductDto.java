package com.mini_project.redis_serialize.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductDto(long id,
                         String name,
                         String description,
                         List<String> tags,
                         Integer price,
                         LocalDateTime createdAt) {
}
