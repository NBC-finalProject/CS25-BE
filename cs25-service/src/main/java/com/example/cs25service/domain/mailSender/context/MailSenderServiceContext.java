package com.example.cs25service.domain.mailSender.context;

import com.example.cs25entity.domain.quiz.entity.Quiz;
import com.example.cs25entity.domain.subscription.entity.Subscription;
import com.example.cs25service.domain.mailSender.MailSenderServiceStrategy;
import com.example.cs25service.domain.mailSender.exception.MailSenderExceptionCode;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailSenderServiceContext {
    private final Map<String, MailSenderServiceStrategy> strategyMap;

    public void send(String toEmail, String code, String strategyKey) {
        MailSenderServiceStrategy strategy = strategyMap.get(strategyKey);
        if (strategy == null) {
            throw new IllegalArgumentException(
                MailSenderExceptionCode.NOT_FOUND_STRATEGY.getMessage() + ": " + strategyKey
            );
        }
        strategy.sendVerificationCodeMail(toEmail, code);
    }

    public void sendQuizMail(Subscription subscription, Quiz quiz){
        MailSenderServiceStrategy strategy = strategyMap.get("sesServiceMailSender");
        strategy.sendQuizMail(subscription, quiz);
    }
}
