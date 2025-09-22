package com.warehouse.warehouse_management.dto;

import java.time.LocalDate;
import java.util.List;

public record AvailableDaysResponse(
        Long orderNumber,
        int daysRequested,
        List<LocalDate> availableDays
) {}