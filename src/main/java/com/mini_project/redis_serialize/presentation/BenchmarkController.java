package com.mini_project.redis_serialize.presentation;

import com.mini_project.redis_serialize.domain.service.BenchmarkService;
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

    private final BenchmarkService benchmarkService;

    /**
     * 배치 저장 → 배치 조회 → 메모리 사용량 순서로 실행,
     * 최종 결과를 JSON으로 리턴함.
     *
     * 요청 예시:
     * POST /benchmark
     * Content-Type: application/json
     *
     * {
     *   "serializationType": "json",
     *   "threadCount": 6,
     *   "count": 1000
     * }
     *
     * "serializationType"으로 가능한 타입 : "json", "gzip", "kryo", "protobuf", "msgpack"
     * "threadCount" : 양수, 최대 6
     * "count" : 양수, 최대 2000
     *
     * 응답 예시:
     * {
     *   "data": {
     *     "serializationType": "json",
     *     "threadCount": 6,
     *     "count": 1000,
     *     "saveElapsedMillis": 150,
     *     "readElapsedMillis": 80,
     *     "totalMemoryBytes": 512000
     *   }
     * }
     */
    @PostMapping("/benchmark")
    public Response<ResponseDto> benchmark(
            @RequestBody @Valid BatchSaveRequest request
    ) {
        ResponseDto dto = benchmarkService.runBenchmark(request);
        return Response.<ResponseDto>builder()
                .data(dto)
                .build();
    }
}