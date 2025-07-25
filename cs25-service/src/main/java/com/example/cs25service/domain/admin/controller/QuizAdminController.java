package com.example.cs25service.domain.admin.controller;

import com.example.cs25common.global.dto.ApiResponse;
import com.example.cs25entity.domain.quiz.enums.QuizFormatType;
import com.example.cs25service.domain.admin.dto.request.QuizCreateRequestDto;
import com.example.cs25service.domain.admin.dto.request.QuizUpdateRequestDto;
import com.example.cs25service.domain.admin.dto.response.QuizDetailDto;
import com.example.cs25service.domain.admin.service.QuizAdminService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/quizzes")
public class QuizAdminController {

    private final QuizAdminService quizAdminService;

    /**
     * 문제 JSON 형식 업로드 컨트롤러
     *
     * @param file         파일 객체
     * @param categoryType 카테고리 타입
     * @param formatType   포맷 타입
     * @return 상태 텍스트를 반환
     */
    @PostMapping("/upload")
    public ApiResponse<String> uploadQuizByJsonFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam("categoryType") String categoryType,
        @RequestParam("formatType") QuizFormatType formatType
    ) {
        if (file.isEmpty()) {
            return new ApiResponse<>(400, "파일이 비어있습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals(MediaType.APPLICATION_JSON_VALUE)) {
            return new ApiResponse<>(400, "JSON 파일만 업로드 가능합니다.");
        }

        quizAdminService.uploadQuizJson(file, categoryType, formatType);
        return new ApiResponse<>(200, "문제 등록 성공");
    }

    /**
     * 관리자 문제 목록 조회 컨트롤러 (기본값: 비추천/오름차순)
     *
     * @param page 페이징 객체
     * @param size 몇개씩 불러올지
     * @return 문제 목록 DTO를 반환
     */
    @GetMapping
    public ApiResponse<Page<QuizDetailDto>> getQuizDetails(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "30") int size
    ) {
        return new ApiResponse<>(200, quizAdminService.getAdminQuizDetails(page, size));
    }

    /**
     * 관리자 문제 상세 조회 컨트롤러
     *
     * @param quizId 문제 id
     * @return 문제 목록 DTO를 반환
     */
    @GetMapping("/{quizId}")
    public ApiResponse<QuizDetailDto> getQuizDetail(
        @Positive @PathVariable(name = "quizId") Long quizId
    ) {
        return new ApiResponse<>(200, quizAdminService.getAdminQuizDetail(quizId));
    }

    /**
     * 관리자 문제 등록 컨트롤러
     *
     * @param requestDto 요청 DTO
     * @return 등록한 문제 id를 반환
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<Long> createQuiz(
        @RequestBody QuizCreateRequestDto requestDto
    ) {
        return new ApiResponse<>(201, quizAdminService.createQuiz(requestDto));
    }

    /**
     * 관리자 문제 수정 컨트롤러
     *
     * @param quizId     문제 id
     * @param requestDto 요청 DTO
     * @return 수정한 문제 DTO를 반환
     */
    @PatchMapping("/{quizId}")
    public ApiResponse<QuizDetailDto> updateQuiz(
        @Positive @PathVariable(name = "quizId") Long quizId,
        @RequestBody QuizUpdateRequestDto requestDto
    ) {
        return new ApiResponse<>(200, quizAdminService.updateQuiz(quizId, requestDto));
    }

    /**
     * 관리자 문제 삭제 컨트롤러
     *
     * @param quizId 문제 id
     * @return 반환값 없음
     */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{quizId}")
    public ApiResponse<Void> deleteQuiz(
        @Positive @PathVariable(name = "quizId") Long quizId
    ) {
        quizAdminService.deleteQuiz(quizId);

        return new ApiResponse<>(204);
    }
}
