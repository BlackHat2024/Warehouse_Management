package com.warehouse.warehouse_management.repository;

import com.warehouse.warehouse_management.entity.Order;
import com.warehouse.warehouse_management.entity.OrderStatus;
import com.warehouse.warehouse_management.entity.Priority;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByOrderBySubmittedDateDesc();
    List<Order> findAllByStatusOrderBySubmittedDateDesc(OrderStatus status);
    @EntityGraph(attributePaths = {"items", "items.item"})
    List<Order> findByClientId(Long clientId);
    @EntityGraph(attributePaths = {"items", "items.item"})
    List<Order> findByClientIdAndStatus(Long clientId, OrderStatus status);
    Optional<Order> findByOrderNumberAndClientId(Long orderNumber, Long clientId);
    @EntityGraph(attributePaths = {"items", "items.item", "client", "delivery", "delivery.trucks"})
    Order findWithAllByOrderNumber(Long orderNumber);
    @Query("select o.orderNumber from Order o " +
            "where o.priority = :priority and o.status = :status")
    List<Long> findIdsByPriorityAndStatus(@Param("priority") Priority priority,
                                          @Param("status")   OrderStatus   status);
}

