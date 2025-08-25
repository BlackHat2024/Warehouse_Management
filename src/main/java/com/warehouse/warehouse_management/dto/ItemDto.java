package com.warehouse.warehouse_management.dto;

import java.math.BigDecimal;

public record OrderItemDto(Long itemId,
                           String itemName,
                           Long quantity,
                           BigDecimal price,
                           Long volume) {
}
