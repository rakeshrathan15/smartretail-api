package com.smartretail.inventory.service;

import com.smartretail.catalog.entity.Product;
import com.smartretail.catalog.repository.ProductRepository;
import com.smartretail.common.exception.ApiException;
import com.smartretail.common.exception.ErrorCode;
import com.smartretail.common.util.BusinessCalendar;
import com.smartretail.inventory.dto.request.ReceiptItemRequest;
import com.smartretail.inventory.dto.request.ReceiptRequest;
import com.smartretail.inventory.entity.StockReceipt;
import com.smartretail.inventory.repository.StockReceiptRepository;
import com.smartretail.store.repository.StoreRepository;
import com.smartretail.supplier.entity.Supplier;
import com.smartretail.supplier.repository.SupplierRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Takes in stock from suppliers: raises each product's stock and updates its cost, all or nothing. */
@RequiredArgsConstructor
@Slf4j
@Service
public class StockReceiptService {

    private final StockReceiptRepository receipts;
    private final ProductRepository products;
    private final SupplierRepository suppliers;
    private final StoreRepository stores;
    private final BusinessCalendar calendar;

    @Transactional
    public UUID receive(ReceiptRequest request, UUID adminId) {
        if (request.receiptDate().isAfter(calendar.today())) {
            throw new ApiException(ErrorCode.INVALID_STOCK_RECEIPT, "The receipt date cannot be in the future");
        }
        String invoice = blankToNull(request.invoiceNumber());
        checkSupplier(request.supplierId(), invoice);
        if (request.storeId() != null && !stores.existsById(request.storeId())) {
            throw new ApiException(ErrorCode.STORE_NOT_FOUND, "Store not found");
        }

        Map<UUID, Product> locked = lockProducts(request.items());
        StockReceipt receipt = new StockReceipt(request.supplierId(), request.storeId(), request.receiptDate(), invoice,
                blankToNull(request.notes()), adminId);
        for (ReceiptItemRequest item : request.items()) {
            locked.get(item.productId()).receiveStock(item.quantity(), item.costPrice());
            receipt.addItem(item.productId(), item.quantity(), item.costPrice());
        }
        UUID receiptId = receipts.saveAndFlush(receipt).getId();
        log.info("Stock received: receiptId={} supplierId={} items={} adminId={}", receiptId, request.supplierId(),
                request.items().size(), adminId);
        return receiptId;
    }

    private void checkSupplier(UUID supplierId, String invoice) {
        if (supplierId == null) {
            return;
        }
        Supplier supplier = suppliers.findById(supplierId)
                .orElseThrow(() -> new ApiException(ErrorCode.SUPPLIER_NOT_FOUND, "Supplier not found"));
        if (!supplier.isActive()) {
            throw new ApiException(ErrorCode.SUPPLIER_INACTIVE, supplier.getName() + " is turned off. Turn the supplier on first.");
        }
        if (invoice != null && receipts.existsBySupplierIdAndInvoiceNumberIgnoreCase(supplierId, invoice)) {
            throw new ApiException(ErrorCode.DUPLICATE_SUPPLIER_INVOICE, "Bill " + invoice + " from " + supplier.getName() + " has already been entered");
        }
    }

    /** Locked in id order (see the repository), so two receipts cannot wait on each other. */
    private Map<UUID, Product> lockProducts(List<ReceiptItemRequest> items) {
        List<UUID> ids = items.stream().map(ReceiptItemRequest::productId).toList();
        if (ids.stream().distinct().count() != ids.size()) {
            throw new ApiException(ErrorCode.INVALID_STOCK_RECEIPT, "Each product can be listed only once on a receipt");
        }
        Map<UUID, Product> locked = products.lockAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        if (locked.size() != ids.size()) {
            throw new ApiException(ErrorCode.PRODUCT_NOT_FOUND, "A product on this receipt no longer exists");
        }
        return locked;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
