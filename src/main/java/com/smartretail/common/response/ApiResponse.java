package com.smartretail.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartretail.common.exception.ErrorCode;
import com.smartretail.common.web.RequestLoggingFilter;
import java.time.Instant;
import org.slf4j.MDC;

/** The one shape of every JSON response: {@code data} on success, {@code error} on failure. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ApiError error, Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(ErrorCode code, String message) {
        return error(code, message, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode code, String message, Object details) {
        return new ApiResponse<>(false, null, new ApiError(code.name(), message, details, MDC.get(RequestLoggingFilter.REQUEST_ID)), Instant.now());
    }
}
