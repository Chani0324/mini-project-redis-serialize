package com.mini_project.redis_serialize.domain.service;

import com.mini_project.redis_serialize.infra.TemplateProvider;
import com.mini_project.redis_serialize.presentation.dto.BatchSaveRequest;
import com.mini_project.redis_serialize.presentation.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class MemoryUsageService {

    private final TemplateProvider templateProvider;

    /**
     * INFO MEMORY 명령으로 가져온 프로퍼티들 중 지정한 필드를 반환.
     *
     * 예를 들어:
     *   fieldName = "used_memory" → 실제 Redis 전체 메모리(바이트) 사용량 반환
     *   fieldName = "used_memory_peak" → peak 메모리 사용량
     *   fieldName = "mem_fragmentation_ratio" → 단편화 비율(String으로 리턴된 후 parse)
     *
     * @param serializationType 어느 RedisTemplate을 사용할지(내부적으로는 connection만 사용)
     * @param fieldName         INFO MEMORY 결과에서 가져올 필드 이름
     * @return 해당 필드의 값(문자열을 Long/Double로 파싱해서 반환; 파싱 불가 시 예외)
     */
    public String getMemoryInfoField(String serializationType, String fieldName) {
        RedisTemplate<String, ?> template = templateProvider.getTemplate(serializationType);

        // RedisCallback 사용: info("memory") → Properties 반환
        return template.execute((RedisCallback<String>) connection -> {
            Properties info = connection.info("memory");
            String value = info.getProperty(fieldName);
            if (value == null) {
                throw new IllegalArgumentException("INFO MEMORY에 '" + fieldName + "' 필드가 없습니다.");
            }
            return value;
        });
    }

    /**
     * INFO MEMORY 결과 중 'used_memory' 필드를 Long으로 파싱하여 반환합니다.
     * (전체 Redis 인스턴스가 사용 중인 메모리 바이트)
     */
    public long totalMemoryUsageInfo(BatchSaveRequest request) {
        // "used_memory" 대신 다른 INFO MEMORY 필드를 쓰고 싶으면,
        // getMemoryInfoField(request.getSerializationType(), "필드명")을 호출하면 됩니다.
        String usedMemoryStr = getMemoryInfoField(request.serializationType(), "used_memory");
        return Long.parseLong(usedMemoryStr);
    }
}