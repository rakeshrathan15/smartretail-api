package com.smartretail.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * What went wrong: {@code code} is the stable key clients can branch on, {@code message} is safe to show
 * the user, {@code details} holds extra data such as validation errors by field, and {@code requestId} finds it in the logs.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code, String message, Object details, String requestId) {
}
