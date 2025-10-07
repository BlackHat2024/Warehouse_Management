package com.warehouse.warehouse_management.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity @Table(name="orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="order_number")
    private Long orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="client_id")
    private User client;

    @Column(name="submitted_date")
    private LocalDateTime submittedDate;

    @Column(name = "total")
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private Priority priority;

    @OneToMany(mappedBy="order", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy="order")
    private Delivery delivery;

    @Column(name = "decline_reason")
    private String declineReason;

    // helpers
    public void addItem(OrderItem oi){ items.add(oi); oi.setOrder(this); }
    public void removeItem(OrderItem oi){ items.remove(oi); oi.setOrder(null); }
}
