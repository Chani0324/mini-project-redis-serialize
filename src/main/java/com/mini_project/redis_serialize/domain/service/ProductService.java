package com.mini_project.redis_serialize.domain.service;

import com.mini_project.redis_serialize.presentation.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final RedisTemplate<String, ProductDto> redisTemplate;

    public void saveAll(List<ProductDto> products) {
        for (ProductDto p : products) {
            redisTemplate.opsForValue().set("prod:" + p.id(), p);
        }
    }

    public List<ProductDto> findAll(List<Long> ids) {
        return ids.stream()
                .map(id -> redisTemplate.opsForValue().get("prod:" + id))
                .toList();
    }
}
