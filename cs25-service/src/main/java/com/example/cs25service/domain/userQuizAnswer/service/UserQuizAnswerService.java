package com.example.cs25service.domain.userQuizAnswer.service;

import com.example.cs25entity.domain.quiz.entity.Quiz;
import com.example.cs25entity.domain.quiz.exception.QuizException;
import com.example.cs25entity.domain.quiz.exception.QuizExceptionCode;
import com.example.cs25entity.domain.quiz.repository.QuizRepository;
import com.example.cs25entity.domain.subscription.entity.Subscription;
import com.example.cs25entity.domain.subscription.exception.SubscriptionException;
import com.example.cs25entity.domain.subscription.exception.SubscriptionExceptionCode;
import com.example.cs25entity.domain.subscription.repository.SubscriptionRepository;
import com.example.cs25entity.domain.user.entity.User;
import com.example.cs25entity.domain.user.repository.UserRepository;
import com.example.cs25entity.domain.userQuizAnswer.dto.UserAnswerDto;
import com.example.cs25entity.domain.userQuizAnswer.entity.UserQuizAnswer;
import com.example.cs25entity.domain.userQuizAnswer.exception.UserQuizAnswerException;
import com.example.cs25entity.domain.userQuizAnswer.exception.UserQuizAnswerExceptionCode;
import com.example.cs25entity.domain.userQuizAnswer.repository.UserQuizAnswerRepository;
import com.example.cs25service.domain.userQuizAnswer.dto.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQuizAnswerService {

    private final UserQuizAnswerRepository userQuizAnswerRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * 사용자의 퀴즈 답변을 저장하는 메서드
     * 중복 답변을 방지하고 사용자 정보와 함께 답변을 저장
     * 
     * @param quizSerialId 퀴즈 시리얼 ID (UUID)
     * @param requestDto 사용자 답변 요청 DTO
     * @return 저장된 사용자 퀴즈 답변의 ID
     * @throws SubscriptionException 구독 정보를 찾을 수 없는 경우
     * @throws QuizException 퀴즈를 찾을 수 없는 경우
     * @throws UserQuizAnswerException 중복 답변인 경우
     */
    @Transactional
    public Long submitAnswer(String quizSerialId, UserQuizAnswerRequestDto requestDto) {

        // 구독 정보 조회
        Subscription subscription = subscriptionRepository.findBySerialId(
                requestDto.getSubscriptionId())
            .orElseThrow(() -> new SubscriptionException(
                SubscriptionExceptionCode.NOT_FOUND_SUBSCRIPTION_ERROR));

        // 퀴즈 조회
        Quiz quiz = quizRepository.findBySerialId(quizSerialId)
            .orElseThrow(() -> new QuizException(QuizExceptionCode.NOT_FOUND_ERROR));

        // 중복 답변 제출 막음
        boolean isDuplicate = userQuizAnswerRepository
            .existsByQuizIdAndSubscriptionId(quiz.getId(), subscription.getId());
        if (isDuplicate) {
            throw new UserQuizAnswerException(UserQuizAnswerExceptionCode.DUPLICATED_ANSWER);
        }

        // 유저 정보 조회
        User user = userRepository.findBySubscription(subscription).orElse(null);

        UserQuizAnswer answer = userQuizAnswerRepository.save(
            UserQuizAnswer.builder()
                .userAnswer(requestDto.getAnswer())
                .isCorrect(null)
                .user(user)
                .quiz(quiz)
                .subscription(subscription)
                .build()
        );
        return answer.getId();
    }

    /**
     * 사용자의 퀴즈 답변을 채점하고 결과를 반환하는 메서드
     * 객관식과 주관식 문제를 모두 지원하며, 회원인 경우 점수를 업데이트
     * 
     * @param userQuizAnswerId 사용자 퀴즈 답변 ID
     * @return 채점 결과를 포함한 응답 DTO
     * @throws UserQuizAnswerException 답변을 찾을 수 없는 경우
     */
    @Transactional
    public CheckSimpleAnswerResponseDto evaluateAnswer(Long userQuizAnswerId) {
        UserQuizAnswer userQuizAnswer = userQuizAnswerRepository.findWithQuizAndUserById(userQuizAnswerId).orElseThrow(
                () -> new UserQuizAnswerException(UserQuizAnswerExceptionCode.NOT_FOUND_ANSWER)
        );

        Quiz quiz = userQuizAnswer.getQuiz();
        boolean isAnswerCorrect = getIsAnswerCorrect(quiz, userQuizAnswer);

        userQuizAnswer.updateIsCorrect(isAnswerCorrect);
        return new CheckSimpleAnswerResponseDto(
                quiz.getQuestion(),
                userQuizAnswer.getUserAnswer(),
                quiz.getAnswer(),
                quiz.getCommentary(),
                userQuizAnswer.getIsCorrect()
        );
    }

    /**
     * 특정 퀴즈의 각 선택지별 선택률을 계산하는 메서드
     * 모든 사용자의 답변을 집계하여 통계 정보를 반환
     * 
     * @param quizSerialId 퀴즈 시리얼 ID
     * @return 선택지별 선택률과 총 응답 수를 포함한 응답 DTO
     * @throws QuizException 퀴즈를 찾을 수 없는 경우
     */
    public SelectionRateResponseDto calculateSelectionRateByOption(String quizSerialId) {
        Quiz quiz = quizRepository.findBySerialId(quizSerialId)
            .orElseThrow(() -> new QuizException(QuizExceptionCode.NOT_FOUND_ERROR));
        List<UserAnswerDto> answers = userQuizAnswerRepository.findUserAnswerByQuizId(quiz.getId());

        //보기별 선택 수 집계
        Map<String, Long> selectionCounts = answers.stream()
            .map(UserAnswerDto::getUserAnswer)
            .filter(Objects::nonNull)
            .map(String::trim)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // 총 응답 수 계산
        long totalResponses = selectionCounts.values().stream().mapToLong(Long::longValue).sum();

        // 선택률 계산
        Map<String, Double> selectionRates = selectionCounts.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> (double) entry.getValue() / totalResponses
            ));

        return new SelectionRateResponseDto(selectionRates, totalResponses);
    }

    /**
     * 사용자의 답변이 정답인지 확인하고 점수를 업데이트하는 메서드
     * 채점 로직을 실행한 후 회원인 경우 점수를 업데이트
     * 
     * @param quiz 퀴즈 정보
     * @param userQuizAnswer 사용자 답변 정보
     * @return 답변 정답 여부
     * @throws QuizException 지원하지 않는 퀴즈 타입인 경우
     */
    private boolean getIsAnswerCorrect(Quiz quiz, UserQuizAnswer userQuizAnswer) {
        boolean isAnswerCorrect = checkAnswer(quiz, userQuizAnswer);
        updateUserScore(userQuizAnswer.getUser(), quiz, isAnswerCorrect);
        return isAnswerCorrect;
    }

    /**
     * 퀴즈 타입에 따라 사용자 답변의 정답 여부를 채점하는 메서드
     * - 객관식 (score=1): 사용자 답변과 정답의 첫 글자를 비교
     * - 주관식 (score=3): 사용자 답변과 정답을 공백 제거하여 비교
     * 
     * @param quiz 퀴즈 정보
     * @param userQuizAnswer 사용자 답변 정보
     * @return 답변 정답 여부 (true: 정답, false: 오답)
     * @throws QuizException 지원하지 않는 퀴즈 타입인 경우
     */
    private boolean checkAnswer(Quiz quiz, UserQuizAnswer userQuizAnswer) {
        if(quiz.getType().getScore() == 1){
            // 객관식: 첫 글자만 비교 (예: "1" vs "1번")
            return userQuizAnswer.getUserAnswer().equals(quiz.getAnswer().substring(0, 1));
        }else if(quiz.getType().getScore() == 3){
            // 주관식: 전체 답변을 공백 제거하여 비교
            return userQuizAnswer.getUserAnswer().trim().equals(quiz.getAnswer().trim());
        }else{
            throw new QuizException(QuizExceptionCode.NOT_FOUND_ERROR);
        }
    }

    /**
     * 회원 사용자의 점수를 업데이트하는 메서드
     * 정답/오답 여부와 퀴즈 난이도에 따라 점수를 부여
     * - 정답: 퀴즈 타입 점수 × 난이도 경험치
     * - 오답: 기본 점수 1점
     * 
     * @param user 사용자 정보 (null인 경우 비회원으로 점수 업데이트 안함)
     * @param quiz 퀴즈 정보
     * @param isAnswerCorrect 답변 정답 여부
     */
    private void updateUserScore(User user, Quiz quiz, boolean isAnswerCorrect) {
        if(user != null){
            double updatedScore;
            if(isAnswerCorrect){
                // 정답: 퀴즈 타입 점수 × 난이도 경험치 획득
                updatedScore = user.getScore() + (quiz.getType().getScore() * quiz.getLevel().getExp());
            }else{
                // 오답: 참여 점수 1점 획득
                updatedScore = user.getScore() + 1;
            }
            user.updateScore(updatedScore);
        }
    }
}
