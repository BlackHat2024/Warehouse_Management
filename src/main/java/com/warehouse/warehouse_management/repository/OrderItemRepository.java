package com.warehouse.warehouse_management.repository;

import com.warehouse.warehouse_management.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    Optional<OrderItem> findByOrderOrderNumberAndItemId(Long orderNumber, Long itemId);
    @Query("select coalesce(sum(oi.price), 0) from OrderItem oi where oi.order.orderNumber = :orderNumber")
    BigDecimal total(@Param("orderNumber") Long orderNumber);
}
