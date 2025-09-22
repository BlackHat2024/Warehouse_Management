package com.warehouse.warehouse_management.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Data
@Table(name = "order_items")
public class OrderItem {

    @EmbeddedId
    private OrderItemId id = new OrderItemId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("orderId")
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("itemId")
    @JoinColumn(name = "item_id",
            foreignKey = @ForeignKey(name = "fk_oi_item"))
    private Item item;

    @Column(name = "requested_qty")
    private Long requestedQty;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "volume")
    private Long volume;

}
