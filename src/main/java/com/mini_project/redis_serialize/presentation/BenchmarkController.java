package com.mini_project.redis_serialize.presentation;

import com.mini_project.redis_serialize.domain.service.ProductRedisService;
import com.mini_project.redis_serialize.presentation.dto.BatchSaveRequest;
import com.mini_project.redis_serialize.presentation.dto.Response;
import com.mini_project.redis_serialize.presentation.dto.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BenchmarkController {

    private final ProductRedisService productRedisService;

    /**
     * 배치 저장 엔드포인트
     *
     * 요청 예시:
     * POST /api/v1/products/batch
     * Content-Type: application/json
     *
     * {
     *   "serializationType": "json",
     *   "threadCount": 10,
     *   "count": 1000
     * }
     *
     * - serializationType: "json", "gzip", "kryo", "protobuf", "msgpack" 중 하나
     * - threadCount: Redis 저장 시 사용할 스레드 수
     * - count: 생성해서 저장할 ProductDto 개수
     */
    @PostMapping("/batch")
    public Response<ResponseDto> batchSave(@RequestBody @Valid BatchSaveRequest request) {
        long elapsedMillis = productRedisService.batchSave(request);

        return Response.<ResponseDto>builder()
                .data(ResponseDto.builder()
                        .serializationType(request.serializationType())
                        .threadCount(request.threadCount())
                        .count(request.count())
                        .elapsedMillis(elapsedMillis)
                        .build())
                .build();
    }
}
