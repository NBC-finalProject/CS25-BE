package com.example.cs25batch.batch.component.processor;

import com.example.cs25batch.adapter.RedisStreamsClient;
import com.example.cs25batch.batch.dto.MailDto;
import com.example.cs25batch.batch.service.TodayQuizService;
import com.example.cs25entity.domain.quiz.entity.Quiz;
import com.example.cs25entity.domain.quiz.exception.QuizException;
import com.example.cs25entity.domain.subscription.entity.Subscription;
import com.example.cs25entity.domain.subscription.repository.SubscriptionRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailConsumerAsyncProcessor implements ItemProcessor<Map<String, String>, MailDto> {

    private final SubscriptionRepository subscriptionRepository;
    private final TodayQuizService todayQuizService;
    private final RedisStreamsClient redisClient;

    @Override
    public MailDto process(Map<String, String> message) throws Exception {
        Long subscriptionId = Long.valueOf(message.get("subscriptionId"));
        String recordId = message.get("recordId");

        //long getStart = System.currentTimeMillis();
        Subscription subscription = subscriptionRepository.findByIdOrElseThrow(subscriptionId);
        //long getEnd = System.currentTimeMillis();
        //log.info("[4. 구독 정보 조회] Id : {}, eamil : {}, {}ms", subscriptionId, subscription.getEmail(), getEnd-getStart);

        //MessageQueue에 들어간 후 실제 메일 발송 전에 구독 정보가 변경된 경우에 대한 유효성 검증
        //구독 해지 또는 구독 요일 변경
        //long quizStart = System.currentTimeMillis();
        if (!subscription.isActive() || !subscription.isTodaySubscribed()) {
            return null;
        }

        //Quiz 출제
        try {
            Quiz quiz = todayQuizService.getTodayQuizBySubscription(subscription);
            return MailDto.builder()
                .subscription(subscription)
                .quiz(quiz)
                .recordId(recordId)
                .build();
        } catch(QuizException e){
            //문제 출제 실패로 인한 예외 발생 시, 기존 Queue에 있는 데이터 삭제
            redisClient.ackAndDel(recordId);
            return null;
        }
        //long quizEnd = System.currentTimeMillis();
        //log.info("[5. 문제 출제] QuizId : {} {}ms", quiz.getId(), quizEnd - quizStart);
    }
}
