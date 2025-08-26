package com.example.cs25batch.batch.service;

import com.example.cs25entity.domain.mail.repository.MailLogRepository;
import com.example.cs25entity.domain.quiz.entity.Quiz;
import com.example.cs25entity.domain.quiz.enums.QuizFormatType;
import com.example.cs25entity.domain.quiz.enums.QuizLevel;
import com.example.cs25entity.domain.quiz.exception.QuizException;
import com.example.cs25entity.domain.quiz.exception.QuizExceptionCode;
import com.example.cs25entity.domain.quiz.repository.QuizRepository;
import com.example.cs25entity.domain.subscription.entity.Subscription;
import com.example.cs25entity.domain.subscription.repository.SubscriptionRepository;
import com.example.cs25entity.domain.userQuizAnswer.repository.UserQuizAnswerRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SubscriptionRepository, UserQuizAnswerRepository,QuizAccuracyRedisRepository 참조를 하기때문에 따로 뗏음
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TodayQuizService {

    private final QuizRepository quizRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserQuizAnswerRepository userQuizAnswerRepository;
    private final MailLogRepository mailLogRepository;
    private final SesMailService mailService;

    @Transactional(readOnly = true)
    public Quiz getTodayQuizBySubscription(Subscription subscription) {
        // 1. 구독자 정보 및 카테고리 조회
        Long parentCategoryId = subscription.getCategory().getId(); // 대분류 ID
        Long subscriptionId = subscription.getId();

        // 2. 유저 정답률 계산, 내가 푼 문제 아이디값
        Double accuracyResult = userQuizAnswerRepository.getCorrectRate(subscriptionId,
                parentCategoryId);
        double accuracy = accuracyResult != null ? accuracyResult : 100.0;

        Set<Long> sentQuizIds = mailLogRepository.findDistinctQuiz_IdBySubscription_Id(subscriptionId);
        int quizCount = sentQuizIds.size(); // 사용자가 지금까지 푼 문제 수

        // 6. 서술형 주기 판단 (풀이 횟수 기반)
        boolean isEssayDay = quizCount % 4 == 3; //일단 3배수일때 한번씩은 서술(0,1,2 객관식 / 3서술형)

        QuizFormatType targetType = isEssayDay
                ? QuizFormatType.SUBJECTIVE
                : QuizFormatType.MULTIPLE_CHOICE;

        QuizFormatType alterType = isEssayDay
            ? QuizFormatType.MULTIPLE_CHOICE
            : QuizFormatType.SUBJECTIVE;

        // 3. 정답률 기반 난이도 바운더리 설정
        List<QuizLevel> allowedDifficulties = getAllowedDifficulties(accuracy);

        // 8. 오프셋 계산 (풀이 수 기준)
        long seed = LocalDate.now().toEpochDay() + subscriptionId;
        int offset = (int) (seed % 20);

        // 7. 필터링 조건으로 문제 조회(대분류, 난이도, 내가푼문제 제외, 제외할 카테고리 제외하고, 문제 타입 전부 조건으로)

       /* Quiz todayQuiz = quizRepository.findAvailableQuizzesUnderParentCategory(
                parentCategoryId,
                allowedDifficulties,
                sentQuizIds,
                //excludedCategoryIds,
                targetType,
                offset
        );

        // offset이 너무 커서 결과가 없을 수 있으므로, offset=0으로 한 번 더 조회
        if (todayQuiz == null && offset > 0) {
            todayQuiz = quizRepository.findAvailableQuizzesUnderParentCategory(
                    parentCategoryId,
                    allowedDifficulties,
                    sentQuizIds,
                    targetType,
                    0
            );
        }*/

        int[] offsetsToTry = (offset > 0) ? new int[]{offset, 0} : new int[]{0};
        QuizFormatType[] typesToTry = new QuizFormatType[]{targetType, alterType};

        Quiz todayQuiz = tryFindQuiz(
            parentCategoryId, allowedDifficulties, sentQuizIds, typesToTry, offsetsToTry
        );

        if (todayQuiz == null) {
            log.info("subscription_id {} - 문제 출제 실패, targetType {}", subscriptionId, targetType);
            throw new QuizException(QuizExceptionCode.QUIZ_VALIDATION_FAILED_ERROR);
        }

        return todayQuiz;
    }


    //유저 정답률 기준으로 바운더리 정해줌
    private List<QuizLevel> getAllowedDifficulties(double accuracy) {
        // 난이도 낮
        if (accuracy <= 50.0) {
            return List.of(QuizLevel.EASY);
        } else if (accuracy <= 75.0) { //난이도 중
            return List.of(QuizLevel.EASY, QuizLevel.NORMAL);
        } else { //난이도 상
            return List.of(QuizLevel.EASY, QuizLevel.NORMAL, QuizLevel.HARD);
        }
    }

    private Quiz tryFindQuiz(Long parentCategoryId,
        List<QuizLevel> difficulties,
        Set<Long> sentQuizIds,
        QuizFormatType[] types,
        int[] offsets) {
        for (QuizFormatType type : types) {
            for (int off : offsets) {
                Quiz q = quizRepository.findAvailableQuizzesUnderParentCategory(
                    parentCategoryId, difficulties, sentQuizIds, type, off
                );
                if (q != null) return q;
            }
        }
        return null;
    }

//    private double calculateAccuracy(List<UserQuizAnswer> answers) {
//        if (answers.isEmpty()) {
//            return 100.0;
//        }
//
//        int totalCorrect = 0;
//        for (UserQuizAnswer answer : answers) {
//            if (answer.getIsCorrect()) {
//                totalCorrect++;
//            }
//        }
//        return ((double) totalCorrect / answers.size()) * 100.0;
//    }


    @Transactional
    public void issueTodayQuiz(Long subscriptionId) {
        //해당 구독자의 문제 구독 카테고리 확인
        Subscription subscription = subscriptionRepository.findByIdOrElseThrow(subscriptionId);
        //문제 발급
        Quiz selectedQuiz = getTodayQuizBySubscription(subscription);
        //메일 발송
        mailService.sendQuizEmail(subscription, selectedQuiz);
    }
}
