package com.example.cs25batch.batch.service;

import com.example.cs25entity.domain.mail.entity.MailLog;
import com.example.cs25entity.domain.mail.enums.MailStatus;
import com.example.cs25entity.domain.mail.repository.MailLogRepository;
import com.example.cs25entity.domain.quiz.entity.Quiz;
import com.example.cs25entity.domain.subscription.entity.Subscription;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MailLogBatchService {

    private final MailLogRepository mailLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailLog(Subscription subscription,
        Quiz quiz,
        LocalDateTime sendDateTime,
        String cause) {
        MailLog log = MailLog.builder()
            .subscription(subscription)
            .quiz(quiz)
            .sendDate(sendDateTime)
            .status(MailStatus.FAILED)
            .caused(cause)
            .build();
        mailLogRepository.save(log);
    }

    @Transactional
    public void saveSuccessLog(Subscription subscription,
        Quiz quiz,
        LocalDateTime sendDateTime) {
        mailLogRepository.save(MailLog.builder()
            .subscription(subscription)
            .quiz(quiz)
            .sendDate(sendDateTime)
            .status(MailStatus.SENT)
            .caused(null)
            .build());
    }
}
