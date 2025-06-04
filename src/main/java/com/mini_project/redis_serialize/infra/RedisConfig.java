package com.mini_project.redis_serialize.infra;

import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.mini_project.redis_serialize.presentation.dto.ProductDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mini_project.redis_serialize.presentation.protobuf.Product.ProductDtoMessage;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * RedisConfig: 네 가지 직렬화 방식(JSON, GZIP+JSON, Kryo, Protobuf)별로 RedisTemplate 빈을 등록합니다.
 */
@Configuration
public class RedisConfig {

    // ----------------------------------------
    // 1) JSON 직렬화 (Jackson2JsonRedisSerializer)
    // ----------------------------------------
    @Bean("jsonRedisTemplate")
    @Primary
    public RedisTemplate<String, ProductDto> jsonRedisTemplate(LettuceConnectionFactory cf) {
        RedisTemplate<String, ProductDto> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);

        // Jackson ObjectMapper (LocalDateTime 등 지원)
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<ProductDto> serializer = new Jackson2JsonRedisSerializer<>(mapper, ProductDto.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    // ----------------------------------------
    // 2) GZIP + JSON 직렬화
    // ----------------------------------------
    @Bean("gzipRedisTemplate")
    public RedisTemplate<String, ProductDto> gzipRedisTemplate(LettuceConnectionFactory cf) {
        RedisTemplate<String, ProductDto> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);

        // Jackson ObjectMapper (LocalDateTime 등 지원)
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // 커스텀 RedisSerializer<ProductDto> 정의
        RedisSerializer<ProductDto> gzipSerializer = new RedisSerializer<>() {
            @Override
            public byte[] serialize(ProductDto product) throws SerializationException {
                if (product == null) {
                    return new byte[0];
                }
                try {
                    // 1) ProductDto → JSON 바이트
                    byte[] jsonBytes = mapper.writeValueAsBytes(product);
                    // 2) JSON 바이트를 GZIP으로 압축
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                         GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                        gzip.write(jsonBytes);
                        gzip.finish();
                        return baos.toByteArray();
                    }
                } catch (Exception ex) {
                    throw new SerializationException("GZIP+JSON serialize 오류", ex);
                }
            }

            @Override
            public ProductDto deserialize(byte[] bytes) throws SerializationException {
                if (bytes == null || bytes.length == 0) {
                    return null;
                }
                try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                     GZIPInputStream gzip = new GZIPInputStream(bais)) {
                    // 2) JSON 바이트 → ProductDto
                    return mapper.readValue(gzip, ProductDto.class);
                } catch (Exception ex) {
                    throw new SerializationException("GZIP+JSON deserialize 오류", ex);
                }
            }
        };

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(gzipSerializer);
        template.afterPropertiesSet();
        return template;
    }

    // ----------------------------------------
    // 3) Kryo 직렬화
    // ----------------------------------------
    @Bean("kryoRedisTemplate")
    public RedisTemplate<String, ProductDto> kryoRedisTemplate(LettuceConnectionFactory cf) {
        RedisTemplate<String, ProductDto> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);

        // ThreadLocal로 Kryo 인스턴스를 생성하도록 정의
        ThreadLocal<Kryo> kryoHolder = ThreadLocal.withInitial(() -> {
            Kryo kryo = new Kryo();
            kryo.register(ProductDto.class);
            kryo.register(LocalDateTime.class, new JavaSerializer());
            kryo.register(ArrayList.class);
            return kryo;
        });

        RedisSerializer<ProductDto> kryoSerializer = new RedisSerializer<>() {
            @Override
            public byte[] serialize(ProductDto product) throws SerializationException {
                if (product == null) {
                    return new byte[0];
                }
                Kryo kryo = kryoHolder.get();
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                     Output output = new Output(baos)) {
                    kryo.writeObject(output, product);
                    output.flush();
                    return baos.toByteArray();
                } catch (Exception ex) {
                    throw new SerializationException("Kryo serialize 오류", ex);
                }
            }

            @Override
            public ProductDto deserialize(byte[] bytes) throws SerializationException {
                if (bytes == null || bytes.length == 0) {
                    return null;
                }
                Kryo kryo = kryoHolder.get();
                try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                     Input input = new Input(bais)) {
                    return kryo.readObject(input, ProductDto.class);
                } catch (Exception ex) {
                    throw new SerializationException("Kryo deserialize 오류", ex);
                }
            }
        };

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(kryoSerializer);
        template.afterPropertiesSet();
        return template;
    }

    // ----------------------------------------
    // 4) Protobuf 직렬화
    // ----------------------------------------
    @Bean("protobufRedisTemplate")
    public RedisTemplate<String, ProductDto> protobufRedisTemplate(LettuceConnectionFactory cf) {
        RedisTemplate<String, ProductDto> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);

        RedisSerializer<ProductDto> protobufSerializer = new RedisSerializer<>() {
            @Override
            public byte[] serialize(ProductDto product) throws SerializationException {
                if (product == null) {
                    return new byte[0];
                }
                try {
                    // ProductDto → Protobuf 메시지로 변환
                    ProductDtoMessage.Builder builder =
                            ProductDtoMessage.newBuilder()
                            .setId(product.id())
                            .setName(product.name())
                            .setDescription(product.description())
                            .addAllTags(product.tags())
                            .setPrice(product.price())
                            .setCreatedAt(
                                    product.createdAt()
                                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            );
                    return builder.build().toByteArray();
                } catch (Exception ex) {
                    throw new SerializationException("Protobuf serialize 오류", ex);
                }
            }

            @Override
            public ProductDto deserialize(byte[] bytes) throws SerializationException {
                if (bytes == null || bytes.length == 0) {
                    return null;
                }
                try {
                    ProductDtoMessage msg =
                            ProductDtoMessage.parseFrom(bytes);
                    return new ProductDto(
                            msg.getId(),
                            msg.getName(),
                            msg.getDescription(),
                            msg.getTagsList(),
                            msg.getPrice(),
                            LocalDateTime.parse(
                                    msg.getCreatedAt(),
                                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
                            )
                    );
                } catch (InvalidProtocolBufferException ex) {
                    throw new SerializationException("Protobuf deserialize 오류", ex);
                }
            }
        };

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(protobufSerializer);
        template.afterPropertiesSet();
        return template;
    }

    // 5) MessagePack 직렬화
    @Bean("msgpackRedisTemplate")
    public RedisTemplate<String, ProductDto> msgpackRedisTemplate(LettuceConnectionFactory cf) {
        RedisTemplate<String, ProductDto> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);

        ObjectMapper msgpackMapper = new ObjectMapper(new MessagePackFactory());
        msgpackMapper.registerModule(new JavaTimeModule());

        RedisSerializer<ProductDto> msgpackSerializer = new RedisSerializer<>() {
            @Override
            public byte[] serialize(ProductDto product) throws SerializationException {
                if (product == null) return new byte[0];
                try {
                    return msgpackMapper.writeValueAsBytes(product);
                } catch (Exception ex) {
                    throw new SerializationException("MessagePack serialize 오류", ex);
                }
            }

            @Override
            public ProductDto deserialize(byte[] bytes) throws SerializationException {
                if (bytes == null || bytes.length == 0) return null;
                try {
                    return msgpackMapper.readValue(new ByteArrayInputStream(bytes), ProductDto.class);
                } catch (Exception ex) {
                    throw new SerializationException("MessagePack deserialize 오류", ex);
                }
            }
        };

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(msgpackSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
