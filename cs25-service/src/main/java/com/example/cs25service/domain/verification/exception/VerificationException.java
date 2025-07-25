package com.example.cs25service.domain.verification.exception;

import com.example.cs25common.global.exception.BaseException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class VerificationException extends BaseException {

    private final VerificationExceptionCode errorCode;
    private final HttpStatus httpStatus;
    private final String message;

    public VerificationException(VerificationExceptionCode errorCode) {
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
        this.message = errorCode.getMessage();
    }
}
