package com.example.cs25batch.config;

import com.example.cs25batch.adapter.RedisStreamsClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisStreamsConfig {
    @Bean
    public RedisStreamsClient quizEmailStreamsClient(StringRedisTemplate redisTemplate) {
        return new RedisStreamsClient(
            redisTemplate,
            "quiz-email-stream",   // stream 이름
            "mail-consumer-group", // group
            "mail-worker"          // consumer
        );
    }
}
