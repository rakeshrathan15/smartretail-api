package com.smartretail.order;

import com.smartretail.catalog.CatalogDtos.PageResponse;
import com.smartretail.common.ApiException;
import com.smartretail.order.OrderDtos.OrderDetail;
import com.smartretail.order.OrderDtos.OrderSummary;
import com.smartretail.order.OrderDtos.PlaceOrderRequest;
import com.smartretail.order.OrderDtos.Quote;
import com.smartretail.order.OrderDtos.QuoteRequest;
import com.smartretail.security.CurrentUser;
import com.smartretail.storage.StorageService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
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
@RestController
public class OrderController {

    private final CheckoutService checkout;
    private final OrderRepository orders;
    private final OrderMapper mapper;
    private final StorageService storage;

    public OrderController(CheckoutService checkout, OrderRepository orders, OrderMapper mapper, StorageService storage) {
        this.checkout = checkout;
        this.orders = orders;
        this.mapper = mapper;
        this.storage = storage;
    }

    /**
     * Prices the cart: slab prices, coupon, delivery, minimum order, stock and credit. Changes nothing.
     * Works without login too (no credit shown then), so visitors see real prices.
     */
    @PostMapping("/api/checkout/quote")
    public Quote quote(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody QuoteRequest request) {
        return checkout.quote(jwt == null ? null : CurrentUser.id(jwt), request.items(), request.couponCode());
    }

    @PostMapping("/api/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetail placeOrder(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlaceOrderRequest request) {
        return checkout.placeOrder(CurrentUser.id(jwt), request);
    }

    /**
     * Upload before placing a UPI order; send the returned reference as paymentScreenshot.
     * The image is stored privately: only admins can open it, through the admin orders API.
     */
    @PostMapping(path = "/api/me/payment-screenshots", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> uploadPaymentScreenshot(@RequestPart("file") MultipartFile file) {
        return Map.of("reference", storage.storePrivateImage(file, CheckoutService.PAYMENT_FOLDER));
    }

    @GetMapping("/api/me/orders")
    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> myOrders(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        if (page < 0) {
            throw ApiException.badRequest("page must be 0 or more");
        }
        Page<Order> result = orders.findAllByUserIdOrderByCreatedAtDesc(CurrentUser.id(jwt),
                PageRequest.of(page, Math.clamp(size, 1, 50)));
        return new PageResponse<>(result.getContent().stream().map(OrderSummary::from).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping("/api/me/orders/{id}")
    @Transactional(readOnly = true)
    public OrderDetail myOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return orders.findByIdAndUserId(id, CurrentUser.id(jwt)).map(mapper::toDetail)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
    }
}
