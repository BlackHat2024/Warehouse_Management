package com.warehouse.warehouse_management.controller;

import com.warehouse.warehouse_management.dto.AddItemRequest;
import com.warehouse.warehouse_management.dto.OrderResponse;
import com.warehouse.warehouse_management.dto.UpdateQuantityRequest;
import com.warehouse.warehouse_management.entity.Order;
import com.warehouse.warehouse_management.entity.OrderStatus;
import com.warehouse.warehouse_management.entity.Priority;
import com.warehouse.warehouse_management.mapper.OrderMapper;
import com.warehouse.warehouse_management.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @PostMapping("/orders")
    @Operation(summary = "Create new order")
    public OrderResponse createOrder(@AuthenticationPrincipal Long clientId,
                                     @RequestParam(defaultValue = "NORMAL") Priority priority) {
        Order order = orderService.createOrder(clientId,priority);
        return orderMapper.toDto(order);
    }

    @PostMapping("/orders/{orderId}/items")
    @Operation(summary = "Add item to order")
    public OrderResponse addItem(@AuthenticationPrincipal Long clientId,
                                 @PathVariable Long orderId,
                                 @Valid @RequestBody AddItemRequest req) {
        Order order = orderService.addItemToOrder(clientId, orderId, req.itemId(), req.quantity());
        return orderMapper.toDto(order);
    }

    @PatchMapping("/orders/{orderId}/items/{itemId}")
    @Operation(summary = "Update item quantity")
    public OrderResponse updateItemQty(@AuthenticationPrincipal Long clientId,
                                       @PathVariable Long orderId,
                                       @PathVariable Long itemId,
                                       @Valid @RequestBody UpdateQuantityRequest req) {
        Order order = orderService.updateItemQuantity(clientId, orderId, itemId, req.quantity());
        return orderMapper.toDto(order);
    }

    @DeleteMapping("/orders/{orderId}/items/{itemId}")
    @Operation(summary = "Delete item from order")
    public OrderResponse removeItem(@AuthenticationPrincipal Long clientId,
                                    @PathVariable Long orderId,
                                    @PathVariable Long itemId) {
        Order order = orderService.removeItemFromOrder(clientId, orderId, itemId);
        return orderMapper.toDto(order);
    }

    @PostMapping("/orders/{orderId}/submit")
    @Operation(summary = "Submit order")
    public OrderResponse submitOrder(@AuthenticationPrincipal Long clientId,
                                     @PathVariable Long orderId) {
        Order order = orderService.submitOrder(clientId, orderId);
        return orderMapper.toDto(order);
    }

    @PostMapping("/orders/{orderId}/cancel")
    @Operation(summary = "Cancel order")
    public OrderResponse cancelOrder(@AuthenticationPrincipal Long clientId,
                                     @PathVariable Long orderId) {
        Order order = orderService.cancelOrder(clientId, orderId);
        return orderMapper.toDto(order);
    }

    @GetMapping("/orders")
    @Operation(summary = "List all orders")
    public List<OrderResponse> listMyOrders(@AuthenticationPrincipal Long clientId,
                                            @RequestParam(required = false) OrderStatus status) {
        return orderService.listMyOrders(clientId, status).stream().map(orderMapper::toDto)
                .toList();
    }
}