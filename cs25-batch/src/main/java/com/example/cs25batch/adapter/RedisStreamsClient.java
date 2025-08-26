package com.example.cs25batch.adapter;

import jakarta.annotation.Nullable;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

@RequiredArgsConstructor
public class RedisStreamsClient {

    private final StringRedisTemplate redisTemplate;
    private final String stream;
    private final String group;
    private final String consumer;

    @Nullable
    public MapRecord<String, Object, Object> readWithConsumerGroup(Duration blockTimeout) {

        StreamReadOptions options = StreamReadOptions.empty().count(1).block(blockTimeout);

        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
            Consumer.from(group, consumer),
            options,
            StreamOffset.create(stream, ReadOffset.lastConsumed())
        );

        return (records == null || records.isEmpty()) ? null : records.get(0);
    }

    public void ack(String recordId) {
        redisTemplate.opsForStream().acknowledge(stream, group, RecordId.of(recordId));
    }

    public void del(String recordId) {
        redisTemplate.opsForStream().delete(stream, RecordId.of(recordId));
    }

    public void ackAndDel(String recordId) {
        RecordId id = RecordId.of(recordId);
        redisTemplate.opsForStream().acknowledge(stream, group, id);
        redisTemplate.opsForStream().delete(stream, id);
    }

    public void addDlq(String dlqStream, Map<String, String> message){
        redisTemplate.opsForStream().add(dlqStream, message);
    }
}
