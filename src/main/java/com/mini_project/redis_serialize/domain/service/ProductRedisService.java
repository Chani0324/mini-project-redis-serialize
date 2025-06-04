package com.mini_project.redis_serialize.domain.service;

import com.mini_project.redis_serialize.presentation.dto.BatchSaveRequest;
import com.mini_project.redis_serialize.presentation.dto.ProductDto;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class ProductRedisService {

    private final RedisTemplate<String, ProductDto> jsonRedisTemplate;
    private final RedisTemplate<String, ProductDto> gzipRedisTemplate;
    private final RedisTemplate<String, ProductDto> kryoRedisTemplate;
    private final RedisTemplate<String, ProductDto> protobufRedisTemplate;
    private final RedisTemplate<String, ProductDto> msgpackRedisTemplate;

    public ProductRedisService(
            @Qualifier("jsonRedisTemplate") RedisTemplate<String, ProductDto> jsonRedisTemplate,
            @Qualifier("gzipRedisTemplate") RedisTemplate<String, ProductDto> gzipRedisTemplate,
            @Qualifier("kryoRedisTemplate") RedisTemplate<String, ProductDto> kryoRedisTemplate,
            @Qualifier("protobufRedisTemplate") RedisTemplate<String, ProductDto> protobufRedisTemplate,
            @Qualifier("msgpackRedisTemplate") RedisTemplate<String, ProductDto> msgpackRedisTemplate
    ) {
        this.jsonRedisTemplate = jsonRedisTemplate;
        this.gzipRedisTemplate = gzipRedisTemplate;
        this.kryoRedisTemplate = kryoRedisTemplate;
        this.protobufRedisTemplate = protobufRedisTemplate;
        this.msgpackRedisTemplate = msgpackRedisTemplate;
    }

    /**
     * Batch 저장 메서드
     *
     * @param request 가져올 파라미터:
     *        - serializationType: "json"/"gzip"/"kryo"/"protobuf"/"msgpack"
     *        - threadCount: 사용할 스레드 수
     *        - count: 저장할 상품 개수
     * @return 실제 저장에 걸린 시간(밀리초 단위)
     */
    public long batchSave(BatchSaveRequest request) {
        RedisTemplate<String, ProductDto> template = selectTemplate(request.serializationType());
        if (template == null) {
            throw new IllegalArgumentException("지원하지 않는 serializationType: " + request.serializationType());
        }

        int threadCount = request.threadCount();
        int count = request.count();

        // 1) count 개수만큼 ProductDto 더미 리스트 생성
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

        // 2) 고정 크기 스레드 풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(count);

        // 3) 저장 시작 시간 기록
        long startTime = System.nanoTime();

        // 4) 각 ProductDto마다 스레드 풀에 저장 작업
        for (ProductDto product : products) {
            executor.execute(() -> {
                template.opsForValue().set("product:" + product.id(), product);
                latch.countDown();
            });
        }

        // 5) 모든 작업이 끝날 때까지 대기
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 6) 종료 시간 기록 및 풀 종료
        long endTime = System.nanoTime();
        executor.shutdown();

        // 경과 시간(밀리초 단위) 반환
        return (endTime - startTime) / 1_000_000;
    }

    /**
     * serializationType 문자열을 보고, 등록된 RedisTemplate 중 하나를 리턴합니다.
     * (소문자 비교)
     */
    private RedisTemplate<String, ProductDto> selectTemplate(String serializationType) {
        return switch (serializationType.toLowerCase()) {
            case "json" -> jsonRedisTemplate;
            case "gzip" -> gzipRedisTemplate;
            case "kryo" -> kryoRedisTemplate;
            case "protobuf" -> protobufRedisTemplate;
            case "msgpack" -> msgpackRedisTemplate;
            default -> null;
        };
    }
}