package com.warehouse.warehouse_management.repository;

import com.warehouse.warehouse_management.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    Optional<OrderItem> findByOrderOrderNumberAndItemId(Long orderNumber, Long itemId);
}
