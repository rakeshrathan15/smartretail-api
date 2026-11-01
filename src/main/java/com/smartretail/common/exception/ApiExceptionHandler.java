package com.smartretail.common.exception;

import com.smartretail.common.response.ApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Renders every error as an {@link ApiResponse}. */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        log.info("Request refused: code={} message={}", ex.getErrorCode(), ex.getMessage());
        return respond(ex.getErrorCode(), ex.getMessage());
    }

    /** A database rule stopped the change, e.g. a duplicate, or a record that others still point to. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Database rule refused a change: {}", ex.getMostSpecificCause().getMessage());
        return respond(ErrorCode.DATA_INTEGRITY_VIOLATION,
                "This could not be saved because it duplicates or is still used by another record");
    }

    /** Security decides these itself; rethrown so they still become 401 and 403. */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public void rethrowSecurity(Exception ex) throws Exception {
        throw ex;
    }

    /** The details go to the log, never to the caller. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return respond(ErrorCode.INTERNAL_ERROR, "Something went wrong on our side. Please try again.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(status).headers(headers)
                .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED, "Validation failed", errors));
    }

    /** Errors raised by Spring itself, such as unreadable JSON or a wrong HTTP method, get the same shape. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = body instanceof ProblemDetail detail ? detail
                : ex instanceof ErrorResponse response ? response.getBody() : null;
        String message = problem != null && problem.getDetail() != null
                ? problem.getDetail()
                : "The request could not be processed";
        return super.handleExceptionInternal(ex, ApiResponse.error(ErrorCode.of(status), message), headers, status,
                request);
    }

    private static ResponseEntity<ApiResponse<Void>> respond(ErrorCode code, String message) {
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(code, message));
    }
}
