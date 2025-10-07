package com.warehouse.warehouse_management.service;

import com.warehouse.warehouse_management.entity.Order;
import com.warehouse.warehouse_management.entity.OrderStatus;
import com.warehouse.warehouse_management.entity.Priority;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {
    Order createOrder(Long clientId, Priority priority);
    Order addItemToOrder(Long clientId, Long orderId, Long itemId, Long quantity);
    Order updateItemQuantity(Long clientId, Long orderId, Long itemId, Long quantity);
    Order removeItemFromOrder(Long clientId, Long orderId, Long itemId);
    Order submitOrder(Long clientId, Long orderId);
    Order cancelOrder(Long clientId, Long orderId);
    List<Order> listMyOrders(Long clientId, OrderStatus status);
}
