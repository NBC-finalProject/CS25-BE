package com.example.cs25service.domain.quiz.controller;

import com.example.cs25common.global.dto.ApiResponse;
import com.example.cs25service.domain.quiz.dto.TodayQuizResponseDto;
import com.example.cs25service.domain.quiz.service.QuizAccuracyCalculateService;
import com.example.cs25service.domain.quiz.service.QuizPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QuizTestController {

    private final QuizAccuracyCalculateService accuracyService;
    private final QuizPageService quizPageService;

    @GetMapping("/accuracyTest")
    public ApiResponse<Void> accuracyTest() {
        accuracyService.calculateAndCacheAllQuizAccuracies();
        return new ApiResponse<>(200);
    }

//
//    @GetMapping("/accuracyTest/{subscriptionId}")
//    public ApiResponse<Void> accuracyTest(
//        @PathVariable Long subscriptionId
//    ) {
//        accuracyService.getTodayQuizBySubscription(subscriptionId);
//        return new ApiResponse<>(200);
//    }

//    @GetMapping("/test/todayQuiz")
//    public ApiResponse<TodayQuizResponseDto> showTodayQuizPage(
//            @RequestParam("quizId") String quizId
//    ) {
//
//        return new ApiResponse<>(
//                200,
//                quizPageService.showTodayQuizPage(quizId)
//        );
//    }

//    @GetMapping("/test/sse")
//    public void testSse(HttpServletResponse response) throws IOException {
//        response.setContentType("text/event-stream");
//        response.setCharacterEncoding("UTF-8");
//
//        PrintWriter writer = response.getWriter();
//        writer.write("data: hello world\n\n");
//        writer.flush();
//    }
}
