package com.mini_project.redis_serialize.domain.service;

import com.mini_project.redis_serialize.infra.TemplateProvider;
import com.mini_project.redis_serialize.presentation.dto.BatchSaveRequest;
import com.mini_project.redis_serialize.presentation.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final TemplateProvider templateProvider;

    /**
     * batch 저장 수행 후 직렬화 + Redis 쓰기 작업에 걸린 시간(ms)을 리턴
     */
    public long batchSave(BatchSaveRequest request) {
        RedisTemplate<String, ProductDto> template =
                templateProvider.getTemplate(request.serializationType());

        int threadCount = request.threadCount();
        int count = request.count();

        // 더미 데이터 생성 (tags 필드를 ArrayList로 만들어 Kryo 호환성 확보)
        List<ProductDto> products = IntStream.range(0, count)
                .mapToObj(i -> new ProductDto(
                        i,
                        "name-" + i,
                        "desc-" + i,
                        new ArrayList<>(List.of("tagA", "tagB")),
                        i * 100,
                        LocalDateTime.now()
                ))
                .toList();

        // 고정 크기 스레드 풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(count);

        long startNano = System.nanoTime();

        // 5) 병렬로 Redis에 저장
        for (ProductDto product : products) {
            executor.execute(() -> {
                template.opsForValue().set(String.valueOf(product.id()), product);
                latch.countDown();
            });
        }

        // 모든 저장 작업이 끝날 때까지 대기
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 스레드 풀 종료
        executor.shutdown();

        // 경과 시간(ms) 계산 후 반환
        long endNano = System.nanoTime();
        return (endNano - startNano) / 1_000_000;
    }
}