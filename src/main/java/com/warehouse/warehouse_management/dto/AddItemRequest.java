package com.warehouse.warehouse_management.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddItemRequest(@NotNull Long itemId,
                             @NotNull @Min(1) Long quantity) {

}
