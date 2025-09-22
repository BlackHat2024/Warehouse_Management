package com.warehouse.warehouse_management.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ItemDto(
        Long itemId,
        @NotBlank String itemName,
        @NotNull @PositiveOrZero Long quantity,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotNull @Positive Long volume
) { }