package com.huijulh.study.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(httpStatus(exception.getCode()))
                .body(new ApiResponse<>(exception.getCode(), exception.getMessage(), null));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception exception) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(ErrorCode.BAD_REQUEST, "请求参数校验失败", null));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException exception) {
        if ("token".equalsIgnoreCase(exception.getHeaderName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(401, "学生认证令牌不能为空", null));
        }
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(ErrorCode.BAD_REQUEST, "请求头缺少 " + exception.getHeaderName(), null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(DataIntegrityViolationException exception) {
        log.warn("Database constraint conflict: {}", exception.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(ErrorCode.BAD_REQUEST, "数据已存在或存在关联冲突", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("Unhandled request failure", exception);
        return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(500, "系统内部错误", null));
    }

    private HttpStatus httpStatus(int code) {
        if (code == 401) return HttpStatus.UNAUTHORIZED;
        if (code == 403) return HttpStatus.FORBIDDEN;
        if (code >= 40400 && code < 40500) return HttpStatus.NOT_FOUND;
        if (code >= 40900 && code < 41000) return HttpStatus.CONFLICT;
        if (code >= 41000 && code < 41100) return HttpStatus.GONE;
        if (code >= 42200 && code < 42300) return HttpStatus.UNPROCESSABLE_ENTITY;
        return HttpStatus.BAD_REQUEST;
    }
}
