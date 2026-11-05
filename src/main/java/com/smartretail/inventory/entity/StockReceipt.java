package com.smartretail.inventory.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/** Stock that arrived from a supplier. A record of what happened, so it is never edited. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "stock_receipts")
public class StockReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "supplier_id", updatable = false)
    @Getter(AccessLevel.NONE)
    private UUID supplierId;

    @Column(name = "store_id", updatable = false)
    @Getter(AccessLevel.NONE)
    private UUID storeId;

    @Column(name = "receipt_date", nullable = false, updatable = false)
    @Getter(AccessLevel.NONE)
    private LocalDate receiptDate;

    @Column(name = "invoice_number", updatable = false)
    @Getter(AccessLevel.NONE)
    private String invoiceNumber;

    @Column(updatable = false)
    @Getter(AccessLevel.NONE)
    private String notes;

    @Column(name = "total_cost", nullable = false, updatable = false)
    @Getter(AccessLevel.NONE)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "created_by", updatable = false)
    @Getter(AccessLevel.NONE)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Getter(AccessLevel.NONE)
    private Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false, updatable = false)
    @Getter(AccessLevel.NONE)
    private List<StockReceiptItem> items = new ArrayList<>();

    public StockReceipt(UUID supplierId, UUID storeId, LocalDate receiptDate, String invoiceNumber, String notes,
            UUID createdBy) {
        this.supplierId = supplierId;
        this.storeId = storeId;
        this.receiptDate = receiptDate;
        this.invoiceNumber = invoiceNumber;
        this.notes = notes;
        this.createdBy = createdBy;
    }

    public void addItem(UUID productId, int quantity, BigDecimal costPrice) {
        StockReceiptItem item = new StockReceiptItem(productId, quantity, costPrice);
        items.add(item);
        totalCost = totalCost.add(item.getTotal());
    }
}
