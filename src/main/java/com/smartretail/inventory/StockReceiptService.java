package com.smartretail.inventory;

import com.smartretail.catalog.Product;
import com.smartretail.catalog.ProductRepository;
import com.smartretail.common.ApiException;
import com.smartretail.common.BusinessCalendar;
import com.smartretail.inventory.InventoryDtos.ReceiptItemRequest;
import com.smartretail.inventory.InventoryDtos.ReceiptRequest;
import com.smartretail.store.StoreRepository;
import com.smartretail.supplier.Supplier;
import com.smartretail.supplier.SupplierRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Takes in stock from suppliers: raises each product's stock and updates its cost, all or nothing. */
@Service
public class StockReceiptService {

    private final StockReceiptRepository receipts;
    private final ProductRepository products;
    private final SupplierRepository suppliers;
    private final StoreRepository stores;
    private final BusinessCalendar calendar;

    public StockReceiptService(StockReceiptRepository receipts, ProductRepository products,
            SupplierRepository suppliers, StoreRepository stores, BusinessCalendar calendar) {
        this.receipts = receipts;
        this.products = products;
        this.suppliers = suppliers;
        this.stores = stores;
        this.calendar = calendar;
    }

    @Transactional
    public UUID receive(ReceiptRequest request, UUID adminId) {
        if (request.receiptDate().isAfter(calendar.today())) {
            throw ApiException.badRequest("The receipt date cannot be in the future");
        }
        String invoice = blankToNull(request.invoiceNumber());
        checkSupplier(request.supplierId(), invoice);
        if (request.storeId() != null && !stores.existsById(request.storeId())) {
            throw ApiException.notFound("Store not found");
        }

        Map<UUID, Product> locked = lockProducts(request.items());
        StockReceipt receipt = new StockReceipt(request.supplierId(), request.storeId(), request.receiptDate(), invoice,
                blankToNull(request.notes()), adminId);
        for (ReceiptItemRequest item : request.items()) {
            locked.get(item.productId()).receiveStock(item.quantity(), item.costPrice());
            receipt.addItem(item.productId(), item.quantity(), item.costPrice());
        }
        return receipts.saveAndFlush(receipt).getId();
    }

    private void checkSupplier(UUID supplierId, String invoice) {
        if (supplierId == null) {
            return;
        }
        Supplier supplier = suppliers.findById(supplierId)
                .orElseThrow(() -> ApiException.notFound("Supplier not found"));
        if (!supplier.isActive()) {
            throw ApiException.badRequest(supplier.getName() + " is turned off. Turn the supplier on first.");
        }
        if (invoice != null && receipts.existsBySupplierIdAndInvoiceNumberIgnoreCase(supplierId, invoice)) {
            throw ApiException.conflict("Bill " + invoice + " from " + supplier.getName() + " has already been entered");
        }
    }

    /** Locked in id order (see the repository), so two receipts cannot wait on each other. */
    private Map<UUID, Product> lockProducts(List<ReceiptItemRequest> items) {
        List<UUID> ids = items.stream().map(ReceiptItemRequest::productId).toList();
        if (ids.stream().distinct().count() != ids.size()) {
            throw ApiException.badRequest("Each product can be listed only once on a receipt");
        }
        Map<UUID, Product> locked = products.lockAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        if (locked.size() != ids.size()) {
            throw ApiException.notFound("A product on this receipt no longer exists");
        }
        return locked;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
