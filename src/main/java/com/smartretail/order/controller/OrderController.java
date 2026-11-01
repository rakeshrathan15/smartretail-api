package com.smartretail.order.controller;

import com.smartretail.common.response.ApiResponse;
import com.smartretail.common.response.PageResponse;
import com.smartretail.order.dto.request.PlaceOrderRequest;
import com.smartretail.order.dto.request.QuoteRequest;
import com.smartretail.order.dto.response.OrderDetail;
import com.smartretail.order.dto.response.OrderSummary;
import com.smartretail.order.dto.response.Quote;
import com.smartretail.order.service.CheckoutService;
import com.smartretail.order.service.MyOrderService;
import com.smartretail.security.CurrentUser;
import com.smartretail.storage.service.StorageService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Cart pricing, placing orders and the signed-in user's order history. */
@RequiredArgsConstructor
@RestController
public class OrderController {

    private final CheckoutService checkout;
    private final MyOrderService myOrders;
    private final StorageService storage;

    /**
     * Prices the cart: slab prices, coupon, delivery, minimum order, stock and credit. Changes nothing.
     * Works without login too (no credit shown then), so visitors see real prices.
     */
    @PostMapping("/api/checkout/quote")
    public ApiResponse<Quote> quote(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody QuoteRequest request) {
        return ApiResponse.ok(checkout.quote(jwt == null ? null : CurrentUser.id(jwt), request.items(), request.couponCode()));
    }

    @PostMapping("/api/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderDetail> placeOrder(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlaceOrderRequest request) {
        return ApiResponse.ok(checkout.placeOrder(CurrentUser.id(jwt), request));
    }

    /**
     * Upload before placing a UPI order; send the returned reference as paymentScreenshot.
     * The image is stored privately: only admins can open it, through the admin orders API.
     */
    @PostMapping(path = "/api/me/payment-screenshots", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, String>> uploadPaymentScreenshot(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(Map.of("reference", storage.storePrivateImage(file, CheckoutService.PAYMENT_FOLDER)));
    }

    @GetMapping("/api/me/orders")
    public ApiResponse<PageResponse<OrderSummary>> myOrders(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(myOrders.list(CurrentUser.id(jwt), page, size));
    }

    @GetMapping("/api/me/orders/{id}")
    public ApiResponse<OrderDetail> myOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.ok(myOrders.get(CurrentUser.id(jwt), id));
    }
}
