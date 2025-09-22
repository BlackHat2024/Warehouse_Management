package com.warehouse.warehouse_management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record ScheduleDeliveryRequest(
        @NotNull LocalDate date,
        @NotNull @Size(min = 1) List<String> truckVins
) {}
