package com.example.cs25service.domain.quiz.service;

import com.example.cs25entity.domain.quiz.entity.Quiz;
import com.example.cs25entity.domain.quiz.exception.QuizException;
import com.example.cs25entity.domain.quiz.exception.QuizExceptionCode;
import com.example.cs25entity.domain.quiz.repository.QuizRepository;
import com.example.cs25service.domain.quiz.dto.QuizCategoryResponseDto;
import com.example.cs25service.domain.quiz.dto.TodayQuizResponseDto;
import java.util.Arrays;
import java.util.List;

import com.example.cs25service.domain.quiz.util.AesUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizPageService {

    private final QuizRepository quizRepository;
    private final AesUtil aesUtil;
    /**
     * 오늘의 문제를 반환해주는 메서드
     * @param quizId 문제 id
     * @return 오늘의 문제 응답 DTO를 반환
     */
    public TodayQuizResponseDto showTodayQuizPage(String quizId) {

        Quiz quiz = quizRepository.findBySerialIdOrElseThrow(quizId);

        if(quiz.getType() == null) {
            throw new QuizException(QuizExceptionCode.QUIZ_TYPE_NOT_FOUND_ERROR);
        }
        return switch (quiz.getType()) {
            case MULTIPLE_CHOICE -> getMultipleQuiz(quiz);
            case SHORT_ANSWER, SUBJECTIVE -> getDescriptiveQuiz(quiz);
        };
    }

    /**
     * 객관식인 오늘의 문제를 만들어서 반환해주는 메서드
     *
     * @param quiz 문제 객체
     * @return 객관식 문제를 DTO로 반환
     */
    private TodayQuizResponseDto getMultipleQuiz(Quiz quiz) {
        List<String> choices = Arrays.stream(quiz.getChoice().split("/"))
            .filter(s -> !s.isBlank())
            .map(String::trim)
            .toList();
        String answerNumber = quiz.getAnswer().split("\\.")[0];

        return TodayQuizResponseDto.builder()
            .question(quiz.getQuestion())
            .choice1(choices.get(0))
            .choice2(choices.get(1))
            .choice3(choices.get(2))
            .choice4(choices.get(3))
            .answerNumber(aesUtil.encrypt(answerNumber))
            .commentary(aesUtil.encrypt(quiz.getCommentary()))
            .quizType(quiz.getType().name())
            .quizLevel(quiz.getLevel().name())
            .category(getQuizCategory(quiz))
            .build();
    }

    /**
     * 주관식인 오늘의 문제를 만들어서 반환해주는 메서드
     *
     * @param quiz 문제 객체
     * @return 주관식 문제를 DTO로 반환
     */
    private TodayQuizResponseDto getDescriptiveQuiz(Quiz quiz) {
        return TodayQuizResponseDto.builder()
            .question(quiz.getQuestion())
            .quizType(quiz.getQuestion())
            .answer(aesUtil.encrypt(quiz.getAnswer()))
            .commentary(aesUtil.encrypt(quiz.getCommentary()))
            .quizType(quiz.getType().name())
            .quizLevel(quiz.getLevel().name())
            .category(getQuizCategory(quiz))
            .build();
    }

    /**
     * 문제분야의 대분류/소분류를 DTO로 만들어서 반환해주는 메서드
     *
     * @param quiz 문제 객체
     * @return 문제분야 대분류/소분류 DTO를 반환
     */
    private QuizCategoryResponseDto getQuizCategory(Quiz quiz) {
        // 대분류만 있을 경우
        if (!quiz.getCategory().isChildCategory()) {
            return QuizCategoryResponseDto.builder()
                .main(quiz.getCategory().getCategoryType())
                .build();
        }
        // 소분류일 경우 (대분류/소분류 존재)
        else {
            return QuizCategoryResponseDto.builder()
                .main(quiz.getCategory().getParent().getCategoryType())
                .sub(quiz.getCategory().getCategoryType())
                .build();
        }
    }
}
