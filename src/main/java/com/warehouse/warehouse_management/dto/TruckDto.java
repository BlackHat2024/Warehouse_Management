package com.warehouse.warehouse_management.dto;

public record TruckDto(String vin,
                       String licensePlate,
                       Long containerVolume,
                       boolean active) {
}
