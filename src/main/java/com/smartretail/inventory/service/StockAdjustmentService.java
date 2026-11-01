package com.smartretail.inventory.service;

import com.smartretail.catalog.entity.Product;
import com.smartretail.catalog.repository.ProductRepository;
import com.smartretail.common.exception.ApiException;
import com.smartretail.common.exception.ErrorCode;
import com.smartretail.inventory.dto.request.AdjustmentRequest;
import com.smartretail.inventory.entity.StockAdjustment.Reason;
import com.smartretail.inventory.entity.StockAdjustment;
import com.smartretail.inventory.repository.StockAdjustmentRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manual stock changes, each with a reason and who made it. */
@RequiredArgsConstructor
@Slf4j
@Service
public class StockAdjustmentService {

    private final StockAdjustmentRepository adjustments;
    private final ProductRepository products;

    @Transactional
    public UUID adjust(AdjustmentRequest request, UUID adminId) {
        String notes = request.notes() == null || request.notes().isBlank() ? null : request.notes().trim();
        if (request.delta() == 0) {
            throw new ApiException(ErrorCode.INVALID_STOCK_ADJUSTMENT, "Enter a change other than zero");
        }
        if (request.reason() == Reason.OTHER && notes == null) {
            throw new ApiException(ErrorCode.INVALID_STOCK_ADJUSTMENT, "Add a note saying why, when the reason is Other");
        }
        Product product = products.lockAllById(List.of(request.productId())).stream().findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found"));
        if (product.getStockQuantity() + request.delta() < 0) {
            throw new ApiException(ErrorCode.INSUFFICIENT_STOCK, "Only " + product.getStockQuantity() + " cases of " + product.getName()
                    + " are in stock");
        }
        product.adjustStock(request.delta());
        UUID adjustmentId = adjustments.saveAndFlush(
                new StockAdjustment(product.getId(), request.delta(), request.reason(), notes, adminId)).getId();
        log.info("Stock adjusted: adjustmentId={} productId={} delta={} reason={} adminId={}", adjustmentId,
                product.getId(), request.delta(), request.reason(), adminId);
        return adjustmentId;
    }
}
