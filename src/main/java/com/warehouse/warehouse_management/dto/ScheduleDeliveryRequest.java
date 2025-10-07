package com.warehouse.warehouse_management.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ScheduleDeliveryRequest(
        @NotNull LocalDate date
) {}
