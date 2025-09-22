package com.warehouse.warehouse_management.dto;

import com.warehouse.warehouse_management.entity.OrderStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long orderNumber,
        String clientName,
        OrderStatus status,
        LocalDateTime submittedDate
) {}
