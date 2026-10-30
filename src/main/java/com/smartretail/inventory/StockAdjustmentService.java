package com.smartretail.inventory;

import com.smartretail.catalog.Product;
import com.smartretail.catalog.ProductRepository;
import com.smartretail.common.ApiException;
import com.smartretail.inventory.InventoryDtos.AdjustmentRequest;
import com.smartretail.inventory.StockAdjustment.Reason;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manual stock changes, each with a reason and who made it. */
@Service
public class StockAdjustmentService {

    private final StockAdjustmentRepository adjustments;
    private final ProductRepository products;

    public StockAdjustmentService(StockAdjustmentRepository adjustments, ProductRepository products) {
        this.adjustments = adjustments;
        this.products = products;
    }

    @Transactional
    public UUID adjust(AdjustmentRequest request, UUID adminId) {
        String notes = request.notes() == null || request.notes().isBlank() ? null : request.notes().trim();
        if (request.delta() == 0) {
            throw ApiException.badRequest("Enter a change other than zero");
        }
        if (request.reason() == Reason.OTHER && notes == null) {
            throw ApiException.badRequest("Add a note saying why, when the reason is Other");
        }
        Product product = products.lockAllById(List.of(request.productId())).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        if (product.getStockQuantity() + request.delta() < 0) {
            throw ApiException.conflict("Only " + product.getStockQuantity() + " cases of " + product.getName()
                    + " are in stock");
        }
        product.adjustStock(request.delta());
        return adjustments.saveAndFlush(
                new StockAdjustment(product.getId(), request.delta(), request.reason(), notes, adminId)).getId();
    }
}
