package com.puyuanmaoshan.platform.exception;

import com.puyuanmaoshan.platform.dto.ApiResponse;
import com.puyuanmaoshan.platform.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity
                .status(errorCode.httpStatus())
                .body(ApiResponse.fail(errorCode, ex.getMessage(), requestId(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError == null
                ? ErrorCode.VALIDATION_ERROR.defaultMessage()
                : fieldError.getField() + ": " + fieldError.getDefaultMessage();
        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.httpStatus())
                .body(ApiResponse.fail(ErrorCode.VALIDATION_ERROR, message, requestId(request)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.httpStatus())
                .body(ApiResponse.fail(ErrorCode.VALIDATION_ERROR, ex.getMessage(), requestId(request)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.httpStatus())
                .body(ApiResponse.fail(ErrorCode.VALIDATION_ERROR, "invalid request body", requestId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Log full stack trace for debugging
        request.setAttribute("javax.servlet.error.exception", ex);
        System.err.println("[Unexpected Error] " + ex.getClass().getName() + ": " + ex.getMessage());
        ex.printStackTrace();
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.httpStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR, ex.getMessage(), requestId(request)));
    }

    private String requestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        return requestId == null || requestId.isBlank() ? "req-server-generated" : requestId;
    }
}