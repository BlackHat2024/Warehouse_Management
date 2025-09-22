package com.warehouse.warehouse_management.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;
@Data
@Embeddable
public class OrderItemId implements Serializable {
    private Long orderId;
    private Long itemId;

    public OrderItemId() {}
    public OrderItemId(Long orderId, Long itemId) {
        this.orderId = orderId;
        this.itemId = itemId;
    }

    // getters/setters

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItemId)) return false;
        OrderItemId that = (OrderItemId) o;
        return Objects.equals(orderId, that.orderId) &&
                Objects.equals(itemId, that.itemId);
    }
    @Override
    public int hashCode() {
        return Objects.hash(orderId, itemId);
    }
}
