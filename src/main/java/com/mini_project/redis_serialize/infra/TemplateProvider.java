package com.mini_project.redis_serialize.infra;

import com.mini_project.redis_serialize.presentation.dto.ProductDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class TemplateProvider {
    private final RedisTemplate<String, ProductDto> jsonTemplate;
    private final RedisTemplate<String, ProductDto> gzipTemplate;
    private final RedisTemplate<String, ProductDto> kryoTemplate;
    private final RedisTemplate<String, ProductDto> protobufTemplate;
    private final RedisTemplate<String, ProductDto> msgpackTemplate;

    private static final String JSON = "json";
    private static final String GZIP = "gzip";
    private static final String KRYO = "kryo";
    private static final String PROTOBUF = "protobuf";
    private static final String MSGPACK = "msgpack";

    public TemplateProvider(
            @Qualifier("jsonRedisTemplate") RedisTemplate<String, ProductDto> jsonTemplate,
            @Qualifier("gzipRedisTemplate") RedisTemplate<String, ProductDto> gzipTemplate,
            @Qualifier("kryoRedisTemplate") RedisTemplate<String, ProductDto> kryoTemplate,
            @Qualifier("protobufRedisTemplate") RedisTemplate<String, ProductDto> protobufTemplate,
            @Qualifier("msgpackRedisTemplate") RedisTemplate<String, ProductDto> msgpackTemplate
    ) {
        this.jsonTemplate = jsonTemplate;
        this.gzipTemplate = gzipTemplate;
        this.kryoTemplate = kryoTemplate;
        this.protobufTemplate = protobufTemplate;
        this.msgpackTemplate = msgpackTemplate;
    }

    public RedisTemplate<String, ProductDto> getTemplate(String serializationType) {
        return switch (serializationType.toLowerCase()) {
            case JSON -> jsonTemplate;
            case GZIP -> gzipTemplate;
            case KRYO -> kryoTemplate;
            case PROTOBUF -> protobufTemplate;
            case MSGPACK -> msgpackTemplate;
            default -> throw new IllegalArgumentException("지원하지 않는 serializationType: " + serializationType);
        };
    }
}
