package com.shofe.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorBody(Instant timestamp, int status, String error, String message, Map<String, String> fields) {}

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorBody> api(ApiException ex) {
        return body(ex.getStatus(), ex.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> fields.put(err.getField(), err.getDefaultMessage()));
        return body(HttpStatus.BAD_REQUEST, "Check the highlighted fields", fields);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorBody> badCredentials(BadCredentialsException ex) {
        return body(HttpStatus.UNAUTHORIZED, "Incorrect email or password", Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorBody> denied(AccessDeniedException ex) {
        return body(HttpStatus.FORBIDDEN, "You do not have access to that", Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorBody> illegal(IllegalArgumentException ex) {
        return body(HttpStatus.BAD_REQUEST, "One of the values is not valid", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> fallback(Exception ex) {
        log.error("Unhandled API error", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.", Map.of());
    }

    private ResponseEntity<ErrorBody> body(HttpStatus status, String message, Map<String, String> fields) {
        ErrorBody payload = new ErrorBody(Instant.now(), status.value(), status.getReasonPhrase(), message, fields);
        return ResponseEntity.status(status).body(payload);
    }
}
