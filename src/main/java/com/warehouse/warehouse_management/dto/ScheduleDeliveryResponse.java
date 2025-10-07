package com.warehouse.warehouse_management.dto;

import java.util.List;

public record ScheduleDeliveryResponse(OrderResponse order, List<String> truckPlates) {
}
