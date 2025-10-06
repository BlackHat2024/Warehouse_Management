package com.warehouse.warehouse_management.dto;

import com.warehouse.warehouse_management.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(Long orderNumber,
                            OrderStatus status,
                            LocalDateTime submittedDate,
                            List<ItemDto> items,
                            BigDecimal total) {
}
