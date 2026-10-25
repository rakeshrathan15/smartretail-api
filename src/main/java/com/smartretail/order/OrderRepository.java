package com.smartretail.order;

import com.smartretail.order.OrderEnums.PaymentMethod;
import com.smartretail.order.OrderEnums.PaymentStatus;
import com.smartretail.order.OrderEnums.Status;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    Page<Order> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    @Query(nativeQuery = true, value = "SELECT nextval('order_number_seq')")
    long nextOrderNumber();

    /** Locks the order so two admins changing it at once cannot, say, return its stock twice. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> lockById(@Param("id") UUID id);

    @EntityGraph(attributePaths = "items")
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findWithItems(@Param("id") UUID id);

    long countByCreatedAtGreaterThanEqual(Instant since);

    long countByStatus(Status status);

    long countByPaymentStatusAndStatusNotIn(PaymentStatus paymentStatus, Collection<Status> excluded);

    @Query("select coalesce(sum(o.total), 0) from Order o where o.createdAt >= :since and o.status not in :excluded")
    BigDecimal revenueSince(@Param("since") Instant since, @Param("excluded") Collection<Status> excluded);

    @Query("select coalesce(sum(o.total), 0) from Order o where o.paymentMethod = :method "
            + "and o.paymentStatus = :paymentStatus and o.status not in :excluded")
    BigDecimal sumUnpaid(@Param("method") PaymentMethod method, @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("excluded") Collection<Status> excluded);
}
