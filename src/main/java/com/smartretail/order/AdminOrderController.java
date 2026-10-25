package com.smartretail.order;

import com.smartretail.catalog.CatalogDtos.PageResponse;
import com.smartretail.common.ApiException;
import com.smartretail.order.AdminOrderDtos.AdminOrderDetail;
import com.smartretail.order.AdminOrderDtos.AdminOrderRow;
import com.smartretail.order.AdminOrderDtos.OrderStats;
import com.smartretail.order.AdminOrderDtos.PaymentReview;
import com.smartretail.order.AdminOrderDtos.StatusChange;
import com.smartretail.order.OrderEnums.PaymentMethod;
import com.smartretail.order.OrderEnums.PaymentStatus;
import com.smartretail.order.OrderEnums.Status;
import jakarta.validation.Valid;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only (enforced in SecurityConfig for /api/admin/**). */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminOrderService service;

    public AdminOrderController(AdminOrderService service) {
        this.service = service;
    }

    /**
     * @param q    order number, shop name, contact name or phone
     * @param from first day (yyyy-MM-dd, India time), inclusive
     * @param to   last day, inclusive
     */
    @GetMapping
    public PageResponse<AdminOrderRow> list(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return service.list(new AdminOrderService.Filter(status, paymentMethod, paymentStatus, q, from, to), page, size);
    }

    @GetMapping("/stats")
    public OrderStats stats() {
        return service.stats();
    }

    @GetMapping("/{id}")
    public AdminOrderDetail get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/{id}/status")
    public AdminOrderDetail changeStatus(@PathVariable UUID id, @Valid @RequestBody StatusChange request) {
        return service.changeStatus(id, request.status());
    }

    @PostMapping("/{id}/payment-review")
    public AdminOrderDetail reviewPayment(@PathVariable UUID id, @Valid @RequestBody PaymentReview request) {
        return service.reviewPayment(id, request.decision());
    }

    /** The UPI screenshot the shop uploaded. Never cached, since it is private. */
    @GetMapping("/{id}/payment-screenshot")
    public ResponseEntity<Resource> paymentScreenshot(@PathVariable UUID id) {
        Path file = service.paymentScreenshot(id).orElseThrow(() -> ApiException.notFound("No payment screenshot"));
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(resource);
    }
}
