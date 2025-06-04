package com.mini_project.redis_serialize.domain.service;

import com.mini_project.redis_serialize.infra.TemplateProvider;
import com.mini_project.redis_serialize.presentation.dto.BatchSaveRequest;
import com.mini_project.redis_serialize.presentation.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class ReadService {

    private final TemplateProvider templateProvider;

    /**
     * 멀티스레드로 batch 조회(역직렬화) 수행 후 걸린 시간(ms)을 반환합니다.
     *
     * - threadCount: 몇 개 스레드를 띄워 병렬 조회할지 지정
     * - count: 조회할 키 개수 (product:0 ~ product:count-1)
     *
     * 주의: 네트워크 I/O와 CPU(역직렬화) 비용이 합쳐짐.
     */
    public long batchRead(BatchSaveRequest request) {
        RedisTemplate<String, ProductDto> template =
                templateProvider.getTemplate(request.serializationType());

        int threadCount = request.threadCount();
        int count = request.count();

        // 1) 고정 크기 스레드 풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(count);

        // 2) 조회 시작 시간 기록
        long startNano = System.nanoTime();

        // 3) 각 키마다 GET을 스레드 풀에 제출
        for (int i = 0; i < count; i++) {
            String key = "product:" + i;
            executor.execute(() -> {
                ProductDto dto = template.opsForValue().get(key);
                latch.countDown();
            });
        }

        // 4) 모든 작업이 끝날 때까지 대기
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 5) 스레드 풀 종료
        executor.shutdown();

        // 6) 경과 시간(ms) 계산 후 반환
        long endNano = System.nanoTime();
        return (endNano - startNano) / 1_000_000;
    }
}