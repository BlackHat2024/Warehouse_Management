package com.warehouse.warehouse_management.service;

import com.warehouse.warehouse_management.dto.*;
import com.warehouse.warehouse_management.entity.OrderStatus;

import java.time.LocalDate;
import java.util.List;

public interface ManagerService {
    // Orders
    List<OrderSummaryResponse> listAllOrders(OrderStatus status);
    OrderResponse getOrderDetails(Long orderNumber);
    OrderResponse approveOrder(Long orderNumber);
    OrderResponse declineOrder(Long orderNumber, String reason);

    // Items
    ItemDto createItem(ItemDto req);
    ItemDto updateItem(Long id, ItemDto req);
    void deleteIfQtyZero(Long id);
    List<ItemDto> listItems();

    // Trucks
    TruckDto createTruck(TruckDto req);
    TruckDto updateTruck(String vin, TruckDto req);
    void deleteTruck(String vin);
    List<TruckDto> listTrucks(boolean onlyActive);

    // Delivery
    OrderResponse scheduleDelivery(Long orderNumber, LocalDate date, List<String> truckVins);
    AvailableDaysResponse getAvailableDaysForDelivery(Long orderNumber, int days);
}
