package com.mini_project.redis_serialize.domain.service;

import com.mini_project.redis_serialize.presentation.dto.BatchSaveRequest;
import com.mini_project.redis_serialize.presentation.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BenchmarkService {

    private final StorageService storageService;
    private final ReadService readService;
    private final MemoryUsageService memoryUsageService;

    /**
     * 1) batch 저장 → 2) batch 조회 → 3) 총 메모리 사용량 계산 순서로 실행하고,
     * 각각의 결과(경과 시간, 메모리 바이트)를 BenchmarkResponseDto에 담아 반환.
     */
    public ResponseDto runBenchmark(BatchSaveRequest request) {
        // 1) 저장(직렬화 + Redis 쓰기) 시간
        long saveTime = storageService.batchSave(request);

        // 2) 조회(역직렬화) 시간
        long readTime = readService.batchRead(request);

        // 3) Redis 메모리 사용량 합계
        long memoryBytes = memoryUsageService.totalMemoryUsageInfo(request);

        // 4) DTO 빌드 후 반환
        return ResponseDto.builder()
                .serializationType(request.serializationType())
                .threadCount(request.threadCount())
                .count(request.count())
                .saveElapsedMillis(saveTime)
                .readElapsedMillis(readTime)
                .totalMemoryBytes(memoryBytes)
                .build();
    }
}

