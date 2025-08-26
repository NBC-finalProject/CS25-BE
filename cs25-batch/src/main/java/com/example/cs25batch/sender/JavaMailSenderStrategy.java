package com.example.cs25batch.sender;

import com.example.cs25batch.batch.dto.MailDto;
import com.example.cs25batch.batch.service.JavaMailService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component("javaBatchMailSender")
@RequiredArgsConstructor
public class JavaMailSenderStrategy implements MailSenderStrategy{
    private final JavaMailService javaMailService;
    private final Bucket bucket = Bucket.builder()
            .addLimit(
                    Bandwidth.builder()
                            .capacity(4)
                            .refillGreedy(2, Duration.ofMillis(500))
                            .build()
            )
            .build();

    @Override
    public void sendQuizMail(MailDto mailDto) {
        javaMailService.sendQuizEmail(mailDto.getSubscription(), mailDto.getQuiz());  // 커스텀 메서드로 정의
    }

    @Override
    public boolean tryConsume(Long num){
        return bucket.tryConsume(num);
    }

    @Override
    public void acquirePermitOrWait(){
        while (true) {
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (probe.isConsumed()) return;

            long nanos = probe.getNanosToWaitForRefill();
            long jitter = TimeUnit.MILLISECONDS.toNanos(
                ThreadLocalRandom.current().nextInt(0, 50)
            );
            LockSupport.parkNanos(Math.min(nanos + jitter, TimeUnit.SECONDS.toNanos(1)));
        }
    }
}
