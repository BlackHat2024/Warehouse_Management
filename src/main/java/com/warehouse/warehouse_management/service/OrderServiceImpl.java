package com.warehouse.warehouse_management.service;

import com.warehouse.warehouse_management.entity.*;
import com.warehouse.warehouse_management.exceptions.BusinessRuleExceptions;
import com.warehouse.warehouse_management.repository.ItemRepository;
import com.warehouse.warehouse_management.repository.OrderItemRepository;
import com.warehouse.warehouse_management.repository.OrderRepository;
import com.warehouse.warehouse_management.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orders;
    private final ItemRepository items;
    private final OrderItemRepository orderItems;
    private final UserRepository users;
    static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.9");
    @Override
    public Order createOrder(Long clientId){
        User client= users.findById(clientId).orElseThrow(()-> new BusinessRuleExceptions("Client Not Found"));
        Order order = new Order();
        order.setClient(client);
        order.setStatus(OrderStatus.CREATED);
        order.setSubmittedDate(LocalDateTime.now());
        return orders.save(order);
    }
    @Override
    public Order addItemToOrder(Long clientId, Long orderId, Long itemId, Long quantity) {
        Order order = findOwnedOrder(clientId, orderId);
        ensureEditable(order);

        Item item = items.findById(itemId)
                .orElseThrow(() -> new BusinessRuleExceptions("Item not found"));

        OrderItem oi = orderItems.findByOrderOrderNumberAndItemId(order.getOrderNumber(), item.getId())
                .orElse(null);

        if (oi == null) {
            oi = new OrderItem();
            oi.setOrder(order);
            oi.setItem(item);
            oi.setRequestedQty(quantity);
            if(quantity >= 100){
                oi.setPrice(calcPrice(item.getUnitPrice().multiply(DISCOUNT_RATE), quantity));
            }
            else {
                oi.setPrice(calcPrice(item.getUnitPrice(), quantity));
            }
            oi.setVolume(calcVolume(item.getPackageVolume(), quantity));
            oi.setId(new OrderItemId(order.getOrderNumber(), item.getId()));
            order.addItem(oi);
        } else {
            long newQty = oi.getRequestedQty() + quantity;
            oi.setRequestedQty(newQty);
            oi.setPrice(calcPrice(item.getUnitPrice(), newQty));
            oi.setVolume(calcVolume(item.getPackageVolume(), newQty));
        }

        return orders.save(order);
    }
    @Override
    public Order updateItemQuantity(Long clientId, Long orderId, Long itemId, Long quantity) {
        Order order = findOwnedOrder(clientId, orderId);
        ensureEditable(order);

        OrderItem oi = orderItems.findByOrderOrderNumberAndItemId(order.getOrderNumber(), itemId)
                .orElseThrow(() -> new BusinessRuleExceptions("Item not present in order"));

        Item item = oi.getItem();
        oi.setRequestedQty(quantity);
        if(quantity >= 100){
            oi.setPrice(calcPrice(item.getUnitPrice().multiply(DISCOUNT_RATE), quantity));
        }
        else {
            oi.setPrice(calcPrice(item.getUnitPrice(), quantity));
        }
        oi.setVolume(calcVolume(item.getPackageVolume(), quantity));

        return orders.save(order);
    }

    @Override
    public Order removeItemFromOrder(Long clientId, Long orderId, Long itemId) {
        Order order = findOwnedOrder(clientId, orderId);
        ensureEditable(order);

        OrderItem oi = orderItems.findByOrderOrderNumberAndItemId(order.getOrderNumber(), itemId)
                .orElseThrow(() -> new BusinessRuleExceptions("Item not present in order"));

        order.removeItem(oi);
        orderItems.delete(oi);

        return orders.save(order);
    }

    @Override
    public Order submitOrder(Long clientId, Long orderId) {
        Order order = findOwnedOrder(clientId, orderId);
        if (!(order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.DECLINED)) {
            throw new BusinessRuleExceptions("Only CREATED or DECLINED orders can be submitted");
        }
        if (order.getItems().isEmpty()) {
            throw new BusinessRuleExceptions("Order must contain at least one item to submit");
        }
        order.setStatus(OrderStatus.AWAITING_APPROVAL);
        order.setSubmittedDate(LocalDateTime.now());
        return orders.save(order);
    }

    @Override
    public Order cancelOrder(Long clientId, Long orderId) {
        Order order = findOwnedOrder(clientId, orderId);
        if (order.getStatus() == OrderStatus.FULFILLED ||
                order.getStatus() == OrderStatus.UNDER_DELIVERY ||
                order.getStatus() == OrderStatus.CANCELED) {
            throw new BusinessRuleExceptions("Order cannot be canceled in its current status");
        }
        order.setStatus(OrderStatus.CANCELED);
        return orders.save(order);
    }

    @Override
    public List<Order> listMyOrders(Long clientId, OrderStatus status) {
        return (status == null)
                ? orders.findByClientId(clientId)
                : orders.findByClientIdAndStatus(clientId, status);
    }


    private Order findOwnedOrder(Long clientId, Long orderId) {
        return orders.findByOrderNumberAndClientId(orderId, clientId)
                .orElseThrow(() -> new BusinessRuleExceptions("Order not found for this client"));
    }

    private void ensureEditable(Order order) {
        if (!(order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.DECLINED)) {
            throw new BusinessRuleExceptions("Order can be modified only in CREATED or DECLINED status");
        }
    }

    private BigDecimal calcPrice(BigDecimal unitPrice, Long qty) {
        return unitPrice.multiply(BigDecimal.valueOf(qty));
    }

    private Long calcVolume(Long packageVolume, Long qty) {
        return packageVolume * qty;
    }

}
