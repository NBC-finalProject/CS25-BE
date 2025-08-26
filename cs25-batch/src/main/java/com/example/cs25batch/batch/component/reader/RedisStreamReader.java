package com.example.cs25batch.batch.component.reader;

import com.example.cs25batch.adapter.RedisStreamsClient;
import com.example.cs25batch.sender.context.MailSenderContext;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component("redisConsumeReader")
public class RedisStreamReader implements ItemReader<Map<String, String>> {

    @Value("${mail.strategy:javaBatchMailSender}")
    private String strategyKey;

    private final RedisStreamsClient redisClient;
    private final MailSenderContext mailSenderContext;

    @Override
    public Map<String, String> read() throws InterruptedException {
        //long start = System.currentTimeMillis();

        while (!mailSenderContext.tryConsume(strategyKey, 1L)) {
            Thread.sleep(200); //토큰을 얻을 때까지 간격을 두고 재시도
        }

        MapRecord<String, Object, Object> msg = redisClient.readWithConsumerGroup(Duration.ofMillis(500));
        //redisTemplate.opsForStream().acknowledge(STREAM, GROUP, msg.getId());

        if(msg == null || msg.getValue().isEmpty()) return null;

        Map<String, String> data = new HashMap<>();
        Object subscriptionId = msg.getValue().get("subscriptionId");
        if (subscriptionId != null) {
            data.put("subscriptionId", subscriptionId.toString());
        }
        data.put("recordId", msg.getId().getValue());

        //long end = System.currentTimeMillis();
        //log.info("[3. Queue에서 꺼내기] {}ms", end - start);
        return data;
    }
}
