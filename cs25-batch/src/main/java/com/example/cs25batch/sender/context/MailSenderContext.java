package com.example.cs25batch.sender.context;

import com.example.cs25batch.batch.dto.MailDto;
import com.example.cs25batch.sender.MailSenderStrategy;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailSenderContext {
    private final Map<String, MailSenderStrategy> strategyMap;

    public void send(MailDto dto, String strategyKey) {
        MailSenderStrategy strategy = getValidStrategy(strategyKey);
        strategy.sendQuizMail(dto);
    }

    public boolean tryConsume(String strategyKey, Long num) {
        MailSenderStrategy strategy = getValidStrategy(strategyKey);
        return strategy.tryConsume(num);
    }

    private MailSenderStrategy getValidStrategy(String strategyKey) {
        MailSenderStrategy strategy = strategyMap.get(strategyKey);
        if (strategy == null) {
            throw new IllegalArgumentException("메일 전략이 존재하지 않습니다: " + strategyKey);
        }
        return strategy;
    }

    public void acquirePermitOrWait(String strategyKey) {
        MailSenderStrategy strategy = getValidStrategy(strategyKey);
        strategy.acquirePermitOrWait();
    }
}
