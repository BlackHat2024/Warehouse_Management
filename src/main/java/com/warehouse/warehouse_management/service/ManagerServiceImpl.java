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
import java.util.Comparator;
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
        if (deleted == 1) return;

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
    public ScheduleDeliveryResponse scheduleDelivery(Long orderNumber, LocalDate date) {
        Order o = orders.findById(orderNumber)
                .orElseThrow(() -> new BusinessRuleExceptions("Order not found"));

        if (o.getStatus() != OrderStatus.APPROVED)
            throw new BusinessRuleExceptions("Only APPROVED orders can be scheduled");
        if (o.getDelivery() != null)
            throw new BusinessRuleExceptions("Order already has a scheduled delivery");

        validateBusinessDate(date);

        if (o.getPriority() == Priority.URGENT) {
            rescheduleLowerPriorityDeliveries(date, o.getPriority());
        }

        List<Truck> free = trucks.findFreeTrucksOn(date, LocalDate.now());
        if (free.isEmpty())
            throw new BusinessRuleExceptions("No trucks are available for " + date);

        long capacity = 0L;
        long orderVolume = calcOrderVolume(o);

        List<Truck> selected = selectTrucksByCapacity(free, orderVolume);
        if (selected.isEmpty()) {
            long total = free.stream().mapToLong(t -> t.getContainerVolume() == null ? 0L : t.getContainerVolume()).sum();
            throw new BusinessRuleExceptions("No combination of trucks can satisfy the volume. Total capacity available: " + total);
        }

        List<String> truckPlates = selected.stream()
                .map(Truck::getLicensePlate)
                .toList();

        capacity = selected.stream().mapToLong(t -> t.getContainerVolume() == null ? 0L : t.getContainerVolume()).sum();

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

        if (isWeekend(date)){
            o.setTotal(o.getTotal().multiply(java.math.BigDecimal.valueOf(1.05)));
        }
        o.setDelivery(d);
        o.setStatus(OrderStatus.UNDER_DELIVERY);
        orders.save(o);

        return new ScheduleDeliveryResponse(orderMapper.toDto(orders.findWithAllByOrderNumber(orderNumber)), truckPlates);
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

            if (o.getPriority() == Priority.URGENT) {
                var urgentDeliveries = deliveriesThatDay.stream()
                        .filter(del -> del.getOrder().getPriority() == Priority.URGENT)
                        .toList();
                
                var bookedUrgentVins = urgentDeliveries.stream()
                        .flatMap(del -> del.getTrucks().stream())
                        .map(Truck::getVin)
                        .collect(java.util.stream.Collectors.toSet());

                long freeCapacity = active.stream()
                        .filter(t -> !bookedUrgentVins.contains(t.getVin()))
                        .mapToLong(Truck::getContainerVolume)
                        .sum();

                if (freeCapacity >= orderVolume) {
                    available.add(d);
                }
            } else {
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
        }

        return new AvailableDaysResponse(orderNumber, days, available);
    }

    private void validateBusinessDate(LocalDate date) {
        if (date.isBefore(LocalDate.now()))
            throw new BusinessRuleExceptions("Date must be today or later");
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

    private List<Truck> selectTrucksByCapacity(List<Truck> free, long orderVolume) {

        List<Truck> trucks = free.stream()
                .filter(t -> t.getContainerVolume() != null && t.getContainerVolume() > 0)
                .sorted(Comparator.comparingLong(Truck::getContainerVolume).reversed())
                .toList();

        if (trucks.isEmpty()) return List.of();

        if (trucks.size() > 25) return greedyBestFit(trucks, orderVolume);

        List<Truck> best = new ArrayList<>();
        long[] bestSum = {Long.MAX_VALUE};
        int[] bestCount = {Integer.MAX_VALUE};

        long totalRemaining = trucks.stream().mapToLong(Truck::getContainerVolume).sum();
        dfsSelect(0, 0L, 0, new ArrayList<>(), trucks, orderVolume, totalRemaining, best, bestSum, bestCount);
        return best;
    }

    private void dfsSelect(int idx, long sum, int cnt, List<Truck> pick,
                           List<Truck> trucks, long target, long rem,
                           List<Truck> best, long[] bestSum, int[] bestCount) {

        if (sum >= target) {
            if (sum < bestSum[0] || (sum == bestSum[0] && cnt < bestCount[0])) {
                bestSum[0] = sum;
                bestCount[0] = cnt;
                best.clear();
                best.addAll(pick);
            }
            return;
        }

        if (sum + rem < target) return;

        if (bestSum[0] != Long.MAX_VALUE && sum >= bestSum[0]) return;

        if (idx == trucks.size()) return;

        Truck t = trucks.get(idx);
        long v = t.getContainerVolume() == null ? 0L : t.getContainerVolume();

        pick.add(t);
        dfsSelect(idx + 1, sum + v, cnt + 1, pick, trucks, target, rem - v, best, bestSum, bestCount);
        pick.remove(pick.size() - 1);

        dfsSelect(idx + 1, sum, cnt, pick, trucks, target, rem - v, best, bestSum, bestCount);
    }

    private List<Truck> greedyBestFit(List<Truck> trucks, long orderVolume) {
        long remaining = orderVolume;
        List<Truck> chosen = new ArrayList<>();

        List<Truck> pool = new ArrayList<>(trucks);
        pool.sort(Comparator.comparingLong(Truck::getContainerVolume).reversed());

        while (remaining > 0 && !pool.isEmpty()) {
            int pickIdx = -1;
            long pickVol = -1;

            for (int i = 0; i < pool.size(); i++) {
                long v = pool.get(i).getContainerVolume();
                if (v <= remaining) { pickIdx = i; pickVol = v; break; }
            }

            if (pickIdx == -1) {
                pickIdx = pool.size() - 1;
                pickVol = pool.get(pickIdx).getContainerVolume();
            }

            Truck t = pool.remove(pickIdx);
            chosen.add(t);
            remaining -= pickVol;
        }

        long sum = chosen.stream().mapToLong(Truck::getContainerVolume).sum();
        return (sum >= orderVolume) ? chosen : List.of();
    }

    private void rescheduleLowerPriorityDeliveries(LocalDate date, Priority newOrderPriority) {
        List<Delivery> lowerPriorityDeliveries = deliveries.lockAllByDateAndPriority(date, Priority.NORMAL);

        if (newOrderPriority == Priority.URGENT) {
            for (Delivery delivery : lowerPriorityDeliveries) {
                Order order = delivery.getOrder();

                LocalDate nextDate = date.plusDays(1);
                while (isWeekend(nextDate) || !canScheduleOnDate(order, nextDate)) {
                    nextDate = nextDate.plusDays(1);
                }

                delivery.setScheduledDate(nextDate);
                deliveries.save(delivery);
            }
        }
    }

    private boolean canScheduleOnDate(Order order, LocalDate date) {
        List<Truck> availableTrucks = trucks.findFreeTrucksOn(date, LocalDate.now());
        long orderVolume = calcOrderVolume(order);
        List<Truck> selected = selectTrucksByCapacity(availableTrucks, orderVolume);
        return !selected.isEmpty();
    }

}
