package com.mini_project.redis_serialize.presentation.dto;

import lombok.Builder;
import org.springframework.http.HttpStatus;

@Builder
public record Response<T>(
        Integer code,
        String message,
        T data
) {
    public Response {
        if (code == null) {
            code = HttpStatus.OK.value();
        }
        if (message == null) {
            message = HttpStatus.OK.getReasonPhrase();
        }
    }
}
