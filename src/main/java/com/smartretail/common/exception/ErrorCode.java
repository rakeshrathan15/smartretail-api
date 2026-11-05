package com.smartretail.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/** Every kind of error the API returns. The name is sent to clients, so never rename a published one. */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    CONFLICT(HttpStatus.CONFLICT),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT),
    PAYLOAD_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),

    // Not found
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND),
    BANNER_NOT_FOUND(HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND),
    EXPENSE_NOT_FOUND(HttpStatus.NOT_FOUND),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND),
    PAYMENT_SCREENSHOT_NOT_FOUND(HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND),
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND),
    RECEIPT_NOT_FOUND(HttpStatus.NOT_FOUND),
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND),
    SHOP_NOT_FOUND(HttpStatus.NOT_FOUND),
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND),
    SUPPLIER_NOT_FOUND(HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND),

    // Rejected input
    ADDRESS_LIMIT_REACHED(HttpStatus.BAD_REQUEST),
    EMAIL_OR_PHONE_REQUIRED(HttpStatus.BAD_REQUEST),
    EXPENSE_DATE_IN_FUTURE(HttpStatus.BAD_REQUEST),
    INVALID_BANNER_ORDER(HttpStatus.BAD_REQUEST),
    INVALID_COUPON_VALUE(HttpStatus.BAD_REQUEST),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST),
    INVALID_IMAGE(HttpStatus.BAD_REQUEST),
    INVALID_PAGE(HttpStatus.BAD_REQUEST),
    INVALID_PHONE(HttpStatus.BAD_REQUEST),
    INVALID_PRICE_SLABS(HttpStatus.BAD_REQUEST),
    INVALID_ROUTE_FILTER(HttpStatus.BAD_REQUEST),
    INVALID_ROUTE_ZONE(HttpStatus.BAD_REQUEST),
    INVALID_STOCK_ADJUSTMENT(HttpStatus.BAD_REQUEST),
    INVALID_STOCK_RECEIPT(HttpStatus.BAD_REQUEST),
    PAYMENT_METHOD_NOT_ALLOWED(HttpStatus.BAD_REQUEST),
    PAYMENT_SCREENSHOT_REQUIRED(HttpStatus.BAD_REQUEST),
    SHOP_ROUTE_LOCKED(HttpStatus.BAD_REQUEST),
    SUPPLIER_INACTIVE(HttpStatus.BAD_REQUEST),

    // Sign-in
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED),

    // Conflicts with current data
    ACCOUNT_ALREADY_EXISTS(HttpStatus.CONFLICT),
    CATEGORY_ALREADY_EXISTS(HttpStatus.CONFLICT),
    CATEGORY_IN_USE(HttpStatus.CONFLICT),
    COUPON_ALREADY_EXISTS(HttpStatus.CONFLICT),
    COUPON_IN_USE(HttpStatus.CONFLICT),
    COUPON_LIMIT_REACHED(HttpStatus.CONFLICT),
    COUPON_NOT_APPLICABLE(HttpStatus.CONFLICT),
    CREDIT_NOT_ENABLED(HttpStatus.CONFLICT),
    DUPLICATE_SUPPLIER_INVOICE(HttpStatus.CONFLICT),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT),
    INSUFFICIENT_CREDIT(HttpStatus.CONFLICT),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT),
    INVALID_ORDER_STATUS_CHANGE(HttpStatus.CONFLICT),
    NO_PAYMENT_TO_REVIEW(HttpStatus.CONFLICT),
    ORDER_CANNOT_BE_PLACED(HttpStatus.CONFLICT),
    PAYMENT_ALREADY_VOIDED(HttpStatus.CONFLICT),
    PAYMENT_NOT_VERIFIED(HttpStatus.CONFLICT),
    PAYMENT_REJECTED(HttpStatus.CONFLICT),
    PHONE_ALREADY_REGISTERED(HttpStatus.CONFLICT),
    PRICE_CHANGED(HttpStatus.CONFLICT),
    ROUTE_ALREADY_EXISTS(HttpStatus.CONFLICT),
    ROUTE_IN_USE(HttpStatus.CONFLICT),
    STORE_ALREADY_EXISTS(HttpStatus.CONFLICT),
    STORE_IN_USE(HttpStatus.CONFLICT),

    // Limits
    TOO_MANY_LOGIN_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS);

    private final HttpStatus status;

    /** The general code for a status, for errors raised by the framework. Specific codes come after the general ones. */
    public static ErrorCode of(HttpStatusCode status) {
        for (ErrorCode code : values()) {
            if (code.status.value() == status.value()) {
                return code;
            }
        }
        return status.is5xxServerError() ? INTERNAL_ERROR : BAD_REQUEST;
    }
}
