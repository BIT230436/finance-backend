package com.example.financebackend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        logger.warn("Validation error: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String field = err instanceof FieldError ? ((FieldError) err).getField() : err.getObjectName();
            errors.put(field, err.getDefaultMessage());
        });
        logger.debug("Validation errors: {}", errors);
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Yêu cầu không hợp lệ", request, errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        logger.warn("IllegalArgumentException: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex, WebRequest request) {
        // IllegalStateException thường xảy ra khi không có authentication
        // Trả về 401 Unauthorized thay vì 403
        logger.warn("IllegalStateException (Authentication issue): {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex, WebRequest request) {
        // Handle OAuth2 endpoint not found (when OAuth2 is not configured)
        if (ex.getMessage() != null && ex.getMessage().contains("oauth2")) {
            logger.debug("OAuth2 endpoint not found (OAuth2 may not be configured): {}", ex.getMessage());
            return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "OAuth2 không được cấu hình. Vui lòng đăng nhập bằng email/password.", request, null);
        }
        logger.warn("Resource not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Tài nguyên không tồn tại", request, null);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex, WebRequest request) {
        // Check if it's a wrapped IllegalArgumentException
        if (ex.getCause() instanceof IllegalArgumentException) {
            logger.warn("RuntimeException with IllegalArgumentException cause: {}", ex.getMessage());
            return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getCause().getMessage(), request, null);
        }
        logger.error("RuntimeException occurred", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Đã xảy ra lỗi không mong muốn: " + ex.getMessage(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex, WebRequest request) {
        logger.error("Unhandled exception occurred", ex);
        String message = ex.getMessage() != null ? ex.getMessage() : "Đã xảy ra lỗi không mong muốn";
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", message, request, null);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String errorCode, String message, WebRequest request, Object details) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", status.value());
        body.put("errorCode", errorCode);
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        if (details != null) body.put("details", details);
        return ResponseEntity.status(status).body(body);
    }
}
