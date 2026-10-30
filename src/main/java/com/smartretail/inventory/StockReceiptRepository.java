package com.smartretail.inventory;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReceiptRepository extends JpaRepository<StockReceipt, UUID> {

    boolean existsBySupplierIdAndInvoiceNumberIgnoreCase(UUID supplierId, String invoiceNumber);
}
