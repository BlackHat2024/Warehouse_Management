package com.warehouse.warehouse_management.service;

import com.warehouse.warehouse_management.dto.*;
import com.warehouse.warehouse_management.entity.*;
import com.warehouse.warehouse_management.exceptions.BusinessRuleExceptions;
import com.warehouse.warehouse_management.exceptions.ItemNotDeletableException;
import com.warehouse.warehouse_management.exceptions.ItemNotFoundException;
import com.warehouse.warehouse_management.mapper.ItemMapper;
import com.warehouse.warehouse_management.mapper.OrderMapper;
import com.warehouse.warehouse_management.mapper.OrderSummaryMapper;
import com.warehouse.warehouse_management.mapper.TruckMapper;
import com.warehouse.warehouse_management.repository.DeliveryRepository;
import com.warehouse.warehouse_management.repository.ItemRepository;
import com.warehouse.warehouse_management.repository.OrderRepository;
import com.warehouse.warehouse_management.repository.TruckRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ManagerServiceImpl implements ManagerService {

    private final OrderRepository orders;
    private final ItemRepository items;
    private final TruckRepository trucks;
    private final DeliveryRepository deliveries;

    private final OrderMapper orderMapper;
    private final OrderSummaryMapper orderSummaryMapper;
    private final ItemMapper itemMapper;
    private final TruckMapper truckMapper;

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<OrderSummaryResponse> listAllOrders(OrderStatus status) {
        List<Order> list = (status == null)
                ? orders.findAllByOrderBySubmittedDateDesc()
                : orders.findAllByStatusOrderBySubmittedDateDesc(status);
        return list.stream().map(orderSummaryMapper::toSummary).toList();
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public OrderResponse getOrderDetails(Long orderNumber) {
        Order o = orders.findWithAllByOrderNumber(orderNumber);
        if (o == null) throw new BusinessRuleExceptions("Order not found");
        return orderMapper.toDto(o);
    }

    @Override
    public OrderResponse approveOrder(Long orderNumber) {
        Order o = orders.findById(orderNumber)
                .orElseThrow(() -> new BusinessRuleExceptions("Order not found"));
        if (o.getStatus() != OrderStatus.AWAITING_APPROVAL)
            throw new BusinessRuleExceptions("Only AWAITING_APPROVAL orders can be approved");
        o.setStatus(OrderStatus.APPROVED);
        o.setDeclineReason(null);
        return orderMapper.toDto(orders.save(o));
    }

    @Override
    public OrderResponse declineOrder(Long orderNumber, String reason) {
        Order o = orders.findById(orderNumber)
                .orElseThrow(() -> new BusinessRuleExceptions("Order not found"));
        if (o.getStatus() != OrderStatus.AWAITING_APPROVAL)
            throw new BusinessRuleExceptions("Only AWAITING_APPROVAL orders can be declined");
        o.setStatus(OrderStatus.DECLINED);
        o.setDeclineReason(reason);
        return orderMapper.toDto(orders.save(o));
    }

    @Override
    public ItemDto createItem(ItemDto dto) {
        var entity = itemMapper.toEntity(dto);
        entity.setId(null);
        return itemMapper.toDto(items.save(entity));
    }

    @Override
    public ItemDto updateItem(Long id, ItemDto dto) {
        var entity = items.findById(id)
                .orElseThrow(() -> new BusinessRuleExceptions("Item not found"));
        itemMapper.update(entity, dto);
        return itemMapper.toDto(items.save(entity));
    }

    @Override
    public void deleteIfQtyZero(Long id) {
        int deleted = items.deleteIfQuantityZero(id);
        if (deleted == 1) return; // success

        if (!items.existsById(id)) {
            throw new ItemNotFoundException(id);
        }
        throw new ItemNotDeletableException("Cannot delete item " + id + " because quantity is not 0.");
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ItemDto> listItems() {
        return items.findAll().stream().map(itemMapper::toDto).toList();
    }

    @Override
    public TruckDto createTruck(TruckDto req) {
        if (trucks.existsById(req.vin())) throw new BusinessRuleExceptions("Truck VIN already exists");
        Truck t = truckMapper.toEntity(req);
        return truckMapper.toDto(trucks.save(t));
    }

    @Override
    public TruckDto updateTruck(String vin, TruckDto req) {
        Truck t = trucks.findById(vin).orElseThrow(() -> new BusinessRuleExceptions("Truck not found"));
        truckMapper.update(t, req);
        t.setVin(vin);
        return truckMapper.toDto(trucks.save(t));
    }

    @Override
    public void deleteTruck(String vin) {
        if (!trucks.existsById(vin)) throw new BusinessRuleExceptions("Truck not found");
        trucks.deleteById(vin);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<TruckDto> listTrucks(boolean onlyActive) {
        var list = onlyActive ? trucks.findAllByActiveTrue() : trucks.findAll();
        return list.stream().map(truckMapper::toDto).toList();
    }

    @Override
    public OrderResponse scheduleDelivery(Long orderNumber, LocalDate date, List<String> truckVins) {
        Order o = orders.findById(orderNumber)
                .orElseThrow(() -> new BusinessRuleExceptions("Order not found"));

        if (o.getStatus() != OrderStatus.APPROVED)
            throw new BusinessRuleExceptions("Only APPROVED orders can be scheduled");
        if (o.getDelivery() != null)
            throw new BusinessRuleExceptions("Order already has a scheduled delivery");

        validateBusinessDate(date);

        if (truckVins == null || truckVins.isEmpty())
            throw new BusinessRuleExceptions("Select at least one truck");

        var uniqueVins = new java.util.LinkedHashSet<>(truckVins);
        var selected = trucks.findAllByVinIn(uniqueVins);
        if (selected.size() != uniqueVins.size()) {
            var found = selected.stream().map(Truck::getVin).collect(java.util.stream.Collectors.toSet());
            var missing = uniqueVins.stream().filter(v -> !found.contains(v)).toList();
            throw new BusinessRuleExceptions("Trucks not found: " + String.join(", ", missing));
        }

        for (Truck t : selected) {
            if (!t.isActive())
                throw new BusinessRuleExceptions("Truck " + t.getVin() + " is inactive");
            if (deliveries.isTruckBooked(date, t.getVin()))
                throw new BusinessRuleExceptions("Truck " + t.getVin() + " is already booked for " + date);
        }

        long orderVolume = calcOrderVolume(o);
        long capacity = selected.stream().mapToLong(Truck::getContainerVolume).sum();
        if (capacity < orderVolume)
            throw new BusinessRuleExceptions("Selected trucks capacity (" + capacity + ") is less than order volume (" + orderVolume + ")");

        for (OrderItem oi : o.getItems()) {
            Item it = oi.getItem();
            if (it == null)
                throw new BusinessRuleExceptions("Order contains an item that no longer exists (itemId=" +
                        (oi.getId() != null ? oi.getId().getItemId() : "unknown") + ")");
            Long need = oi.getRequestedQty();
            if (need == null || need <= 0)
                throw new BusinessRuleExceptions("Order has a line with non-positive quantity (itemId=" + it.getId() + ")");
            long have = it.getQuantity() == null ? 0L : it.getQuantity();
            if (have < need)
                throw new BusinessRuleExceptions("Insufficient stock for item " + it.getId() + " (" + it.getName() + ")");
        }
        for (OrderItem oi : o.getItems()) {
            Item it = oi.getItem();
            long have = it.getQuantity() == null ? 0L : it.getQuantity();
            it.setQuantity(have - oi.getRequestedQty());
            items.save(it);
        }

        Delivery d = new Delivery();
        d.setOrder(o);
        d.setScheduledDate(date);
        selected.forEach(d::addTruck);
        deliveries.saveAndFlush(d);

        selected.forEach(t -> t.setActive(false));
        trucks.saveAll(selected);

        o.setDelivery(d);
        o.setStatus(OrderStatus.UNDER_DELIVERY);
        orders.save(o);

        return orderMapper.toDto(orders.findWithAllByOrderNumber(orderNumber));
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public AvailableDaysResponse getAvailableDaysForDelivery(Long orderNumber, int days) {
        if (days < 1) days = 1;
        if (days > 30) days = 30;

        Order o = orders.findById(orderNumber)
                .orElseThrow(() -> new BusinessRuleExceptions("Order not found"));

        if (o.getStatus() != OrderStatus.APPROVED)
            throw new BusinessRuleExceptions("Available day lookup only for APPROVED orders");

        long orderVolume = calcOrderVolume(o);

        List<Truck> active = trucks.findAllByActiveTrue();
        if (active.isEmpty()) {
            return new AvailableDaysResponse(orderNumber, days, java.util.Collections.emptyList());
        }

        long maxDailyCapacity = active.stream().mapToLong(Truck::getContainerVolume).sum();
        if (maxDailyCapacity < orderVolume) {
            return new AvailableDaysResponse(orderNumber, days, java.util.Collections.emptyList());
        }

        List<LocalDate> available = new ArrayList<>();
        LocalDate start = LocalDate.now().plusDays(1);

        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            if (isWeekend(d)) continue;


            var deliveriesThatDay = deliveries.findByScheduledDate(d);
            var bookedVins = deliveriesThatDay.stream()
                    .flatMap(del -> del.getTrucks().stream())
                    .map(Truck::getVin)
                    .collect(java.util.stream.Collectors.toSet());

            long freeCapacity = active.stream()
                    .filter(t -> !bookedVins.contains(t.getVin()))
                    .mapToLong(Truck::getContainerVolume)
                    .sum();

            if (freeCapacity >= orderVolume) {
                available.add(d);
            }
        }

        return new AvailableDaysResponse(orderNumber, days, available);
    }

    private void validateBusinessDate(LocalDate date) {
        if (date.isBefore(LocalDate.now()))
            throw new BusinessRuleExceptions("Date must be today or later");
        if (isWeekend(date))
            throw new BusinessRuleExceptions("Weekends are off. Pick a weekday.");
    }

    private boolean isWeekend(LocalDate d) {
        DayOfWeek w = d.getDayOfWeek();
        return w == DayOfWeek.SATURDAY || w == DayOfWeek.SUNDAY;
    }
    private long calcOrderVolume(Order o) {
        long total = 0L;
        for (OrderItem oi : o.getItems()) {
            Long v = oi.getVolume();
            if (v == null) {
                Long pkg = (oi.getItem() != null) ? oi.getItem().getPackageVolume() : null;
                Long qty = oi.getRequestedQty();
                if (pkg == null || qty == null)
                    throw new BusinessRuleExceptions("Cannot compute volume for item line; missing packageVolume/quantity");
                v = pkg.longValue() * qty.longValue();
                oi.setVolume(v);
            }
            total += v;
        }
        return total;
    }

}
