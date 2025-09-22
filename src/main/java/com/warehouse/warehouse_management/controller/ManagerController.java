package com.warehouse.warehouse_management.controller;

import com.warehouse.warehouse_management.dto.*;
import com.warehouse.warehouse_management.entity.OrderStatus;
import com.warehouse.warehouse_management.service.ManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/manager")
@RequiredArgsConstructor
@Tag(name = "Manager")
public class ManagerController {

    private final ManagerService manager;

    // ---- Orders ----
    @GetMapping("/orders")
    @Operation(summary = "List of Orders")
    public List<OrderSummaryResponse> listAllOrders(@RequestParam(required = false) OrderStatus status) {
        return manager.listAllOrders(status);
    }

    @GetMapping("/orders/{orderNumber}")
    @Operation(summary = "Single Order")
    public OrderResponse orderDetails(@PathVariable Long orderNumber) {
        return manager.getOrderDetails(orderNumber);
    }

    @PostMapping("/orders/{orderNumber}/approve")
    @Operation(summary = "Approve Order")
    public OrderResponse approveOrder(@PathVariable Long orderNumber) {
        return manager.approveOrder(orderNumber);
    }

    @PostMapping("/orders/{orderNumber}/decline")
    @Operation(summary = "Decline Order")
    public OrderResponse declineOrder(@PathVariable Long orderNumber,
                                 @RequestBody(required = false) ApproveDeclineRequest req) {
        String reason = (req == null) ? null : req.reason();
        return manager.declineOrder(orderNumber, reason);
    }

    // ---- Items ----
    @PostMapping("/items/new")
    @Operation(summary = "Create items")
    public ItemDto createItem(@Valid @RequestBody ItemDto req) {
        return manager.createItem(req);
    }

    @PutMapping("/items/{id}")
    @Operation(summary = "Update item")
    public ItemDto updateItem(@PathVariable Long id, @RequestBody ItemDto dto) {
        return manager.updateItem(id, dto);
    }

    @DeleteMapping("/items/{id}")
    @Operation(summary = "Delete item")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable Long id) {
        manager.deleteIfQtyZero(id);
    }

    @GetMapping("/items")
    @Operation(summary = "List of item")
    public List<ItemDto> listItems() {
        return manager.listItems();
    }

    // ---- Trucks ----
    @PostMapping("/trucks")
    @Operation(summary = "Create Truck")
    public TruckDto createTruck(@Valid @RequestBody TruckDto req) {
        return manager.createTruck(req);
    }

    @PutMapping("/trucks/{vin}")
    @Operation(summary = "Update Truck")
    public TruckDto updateTruck(@PathVariable String vin, @Valid @RequestBody TruckDto req) {
        return manager.updateTruck(vin, req);
    }

    @DeleteMapping("/trucks/{vin}")
    @Operation(summary = "Delete Truck")
    public void deleteTruck(@PathVariable String vin) {
        manager.deleteTruck(vin);
    }

    @GetMapping("/trucks")
    @Operation(summary = "List of Trucks")
    public List<TruckDto> listTrucks(@RequestParam(defaultValue = "false") boolean onlyActive) {
        return manager.listTrucks(onlyActive);
    }

    // ---- Deliveries ----
    @PostMapping("/orders/{orderNumber}/schedule")
    @Operation(summary = "Schedule order")
    public OrderResponse scheduleDelivery(@PathVariable Long orderNumber,
                                  @Valid @RequestBody ScheduleDeliveryRequest req) {
        return manager.scheduleDelivery(orderNumber, req.date(), req.truckVins());
    }

    @GetMapping("/orders/{orderNumber}/available-days")
    @Operation(summary = "Find Available days for scheduling order")
    public AvailableDaysResponse availableDaysForDelivery(@PathVariable Long orderNumber,
                                               @RequestParam(defaultValue = "3") int days) {
        return manager.getAvailableDaysForDelivery(orderNumber, days);
    }
}
