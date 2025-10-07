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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerServiceImplTest {

    @Mock OrderRepository orders;
    @Mock ItemRepository items;
    @Mock TruckRepository trucks;
    @Mock DeliveryRepository deliveries;

    @Mock OrderMapper orderMapper;
    @Mock OrderSummaryMapper orderSummaryMapper;
    @Mock ItemMapper itemMapper;
    @Mock TruckMapper truckMapper;

    @InjectMocks
    ManagerServiceImpl service;

    private static Order orderWithStatus(OrderStatus st) {
        Order o = new Order();
        o.setStatus(st);
        o.setItems(new ArrayList<>());
        return o;
    }
    private static Item newItem(long id, String name, long qty, long pkgVol, BigDecimal price) {
        Item it = new Item();
        it.setId(id);
        it.setName(name);
        it.setQuantity(qty);
        it.setPackageVolume(pkgVol);
        it.setUnitPrice(price);
        return it;
    }
    private static OrderItem oi(long orderId, Item item, long qty, Long volume) {
        OrderItem oi = new OrderItem();
        oi.setId(new OrderItemId(orderId, item.getId()));
        oi.setItem(item);
        oi.setRequestedQty(qty);
        oi.setVolume(volume);
        oi.setPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(qty)));
        return oi;
    }
    private static Truck truck(String vin, boolean active, long cap) {
        Truck t = new Truck();
        t.setVin(vin);
        t.setActive(active);
        t.setContainerVolume(cap);
        return t;
    }
    private static LocalDate nextWeekdayFrom(LocalDate start) {
        LocalDate d = start;
        while (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
            d = d.plusDays(1);
        }
        return d;
    }
    private static Delivery deliveryWithTrucks(String... vins) {
        Delivery d = new Delivery();
        for (String v : vins) {
            Truck t = new Truck();
            t.setVin(v);
            d.addTruck(t);
        }
        return d;
    }

    // ---------- Orders ----------
    @Test
    void listAllOrders_nullStatus_usesFindAllOrdered() {
        var o1 = orderWithStatus(OrderStatus.APPROVED);
        when(orders.findAllByOrderBySubmittedDateDesc()).thenReturn(List.of(o1));
        when(orderSummaryMapper.toSummary(o1)).thenReturn(mock(OrderSummaryResponse.class));

        var out = service.listAllOrders(null);

        assertThat(out).hasSize(1);
        verify(orders).findAllByOrderBySubmittedDateDesc();
        verify(orders, never()).findAllByStatusOrderBySubmittedDateDesc(any());
    }

    @Test
    void listAllOrders_withStatus_usesFindByStatusOrdered() {
        var o1 = orderWithStatus(OrderStatus.AWAITING_APPROVAL);
        when(orders.findAllByStatusOrderBySubmittedDateDesc(OrderStatus.AWAITING_APPROVAL))
                .thenReturn(List.of(o1));
        when(orderSummaryMapper.toSummary(o1)).thenReturn(mock(OrderSummaryResponse.class));

        var out = service.listAllOrders(OrderStatus.AWAITING_APPROVAL);

        assertThat(out).hasSize(1);
        verify(orders).findAllByStatusOrderBySubmittedDateDesc(OrderStatus.AWAITING_APPROVAL);
        verify(orders, never()).findAllByOrderBySubmittedDateDesc();
    }

    @Test
    void getOrderDetails_ok_mapsToDto() {
        Order o = orderWithStatus(OrderStatus.CREATED);
        when(orders.findWithAllByOrderNumber(42L)).thenReturn(o);
        when(orderMapper.toDto(o)).thenReturn(mock(OrderResponse.class));

        var dto = service.getOrderDetails(42L);
        assertThat(dto).isNotNull();
    }

    @Test
    void getOrderDetails_notFound_throws() {
        when(orders.findWithAllByOrderNumber(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.getOrderDetails(99L))
                .isInstanceOf(BusinessRuleExceptions.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void approveOrder_fromAwaitingApproval_setsApproved() {
        Order o = orderWithStatus(OrderStatus.AWAITING_APPROVAL);
        when(orders.findById(1L)).thenReturn(Optional.of(o));
        when(orders.save(o)).thenReturn(o);
        when(orderMapper.toDto(o)).thenReturn(mock(OrderResponse.class));

        var out = service.approveOrder(1L);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.APPROVED);
        assertThat(out).isNotNull();
    }

    @Test
    void approveOrder_wrongStatus_throws() {
        Order o = orderWithStatus(OrderStatus.CREATED);
        when(orders.findById(1L)).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> service.approveOrder(1L))
                .isInstanceOf(BusinessRuleExceptions.class)
                .hasMessageContaining("Only AWAITING_APPROVAL");
    }

    @Test
    void declineOrder_fromAwaitingApproval_setsDeclinedAndReason() {
        Order o = orderWithStatus(OrderStatus.AWAITING_APPROVAL);
        when(orders.findById(2L)).thenReturn(Optional.of(o));
        when(orders.save(o)).thenReturn(o);
        when(orderMapper.toDto(o)).thenReturn(mock(OrderResponse.class));

        var out = service.declineOrder(2L, "bad");
        assertThat(o.getStatus()).isEqualTo(OrderStatus.DECLINED);
        assertThat(out).isNotNull();
    }

    @Test
    void declineOrder_wrongStatus_throws() {
        Order o = orderWithStatus(OrderStatus.CREATED);
        when(orders.findById(2L)).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> service.declineOrder(2L, "x"))
                .isInstanceOf(BusinessRuleExceptions.class)
                .hasMessageContaining("Only AWAITING_APPROVAL");
    }

    // ---------- items ----------
    @Test
    void createItem_mapsAndSaves() {
        var dtoIn = mock(ItemDto.class);
        var entity = new Item();
        when(itemMapper.toEntity(dtoIn)).thenReturn(entity);
        when(items.save(entity)).thenReturn(entity);
        var dtoOut = mock(ItemDto.class);
        when(itemMapper.toDto(entity)).thenReturn(dtoOut);

        var out = service.createItem(dtoIn);

        assertThat(out).isSameAs(dtoOut);
        verify(items).save(entity);
    }

    @Test
    void updateItem_notFound_throws() {
        when(items.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateItem(7L, mock(ItemDto.class)))
                .isInstanceOf(BusinessRuleExceptions.class)
                .hasMessageContaining("Item not found");
    }

    @Test
    void listItems_mapsAll() {
        var i1 = new Item(); var i2 = new Item();
        when(items.findAll()).thenReturn(List.of(i1, i2));
        when(itemMapper.toDto(i1)).thenReturn(mock(ItemDto.class));
        when(itemMapper.toDto(i2)).thenReturn(mock(ItemDto.class));

        var out = service.listItems();
        assertThat(out).hasSize(2);
    }

    @Test
    void deleteIfQtyZero_success_whenRepoReturns1() {
        when(items.deleteIfQuantityZero(5L)).thenReturn(1);
        service.deleteIfQtyZero(5L);
        verify(items, never()).existsById(anyLong());
    }

    @Test
    void deleteIfQtyZero_notFound_throwsItemNotFound() {
        when(items.deleteIfQuantityZero(6L)).thenReturn(0);
        when(items.existsById(6L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteIfQtyZero(6L))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void deleteIfQtyZero_qtyNotZero_throwsItemNotDeletable() {
        when(items.deleteIfQuantityZero(6L)).thenReturn(0);
        when(items.existsById(6L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteIfQtyZero(6L))
                .isInstanceOf(ItemNotDeletableException.class);
    }

    // ---------- trucks ----------
    @Test
    void createTruck_duplicateVin_throws() {
        var req = mock(TruckDto.class);
        when(req.vin()).thenReturn("VIN1");
        when(trucks.existsById("VIN1")).thenReturn(true);

        assertThatThrownBy(() -> service.createTruck(req))
                .isInstanceOf(BusinessRuleExceptions.class)
                .hasMessageContaining("VIN already exists");
    }

    @Test
    void updateTruck_notFound_throws() {
        when(trucks.findById("X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateTruck("X", mock(TruckDto.class)))
                .isInstanceOf(BusinessRuleExceptions.class)
                .hasMessageContaining("Truck not found");
    }

    @Test
    void deleteTruck_notFound_throws() {
        when(trucks.existsById("X")).thenReturn(false);
        assertThatThrownBy(() -> service.deleteTruck("X"))
                .isInstanceOf(BusinessRuleExceptions.class)
                .hasMessageContaining("Truck not found");
    }

    @Test
    void listTrucks_onlyActive_true_callsActiveRepo() {
        when(trucks.findAllByActiveTrue()).thenReturn(List.of());
        var out = service.listTrucks(true);
        assertThat(out).isEmpty();
        verify(trucks).findAllByActiveTrue();
        verify(trucks, never()).findAll();
    }

    @Test
    void listTrucks_onlyActive_false_callsFindAll() {
        when(trucks.findAll()).thenReturn(List.of());
        var out = service.listTrucks(false);
        assertThat(out).isEmpty();
        verify(trucks).findAll();
        verify(trucks, never()).findAllByActiveTrue();
    }

    // ---------- Delivery ----------
//    @Test
//    void scheduleDelivery_happyPath_decrementsStock_andSetsStatus() {
//        Order o = orderWithStatus(OrderStatus.APPROVED);
//        o.setOrderNumber(100L);
//        Item it = newItem(1L, "Box", 10L, 5L, new BigDecimal("2.00"));
//        o.getItems().add(oi(100L, it, 2L, null));
//        when(orders.findById(100L)).thenReturn(Optional.of(o));
//
//        Truck t1 = truck("VIN-A", true, 10L);
//        when(trucks.findAllByVinIn(any())).thenReturn(List.of(t1));
//
//        LocalDate date = nextWeekdayFrom(LocalDate.now().plusDays(1));
//        when(deliveries.isTruckBooked(eq(date), eq("VIN-A"))).thenReturn(false);
//
//        when(items.save(any(Item.class))).thenAnswer(a -> a.getArgument(0));
//        when(orders.save(o)).thenReturn(o);
//        when(orders.findWithAllByOrderNumber(100L)).thenReturn(o);
//        when(orderMapper.toDto(o)).thenReturn(mock(OrderResponse.class));
//
//        var out = service.scheduleDelivery(100L, date, List.of("VIN-A"));
//
//        assertThat(out).isNotNull();
//        assertThat(o.getStatus()).isEqualTo(OrderStatus.UNDER_DELIVERY);
//        assertThat(it.getQuantity()).isEqualTo(8L);
//        verify(deliveries).saveAndFlush(any(Delivery.class));
//    }

//    @Test
//    void scheduleDelivery_wrongStatus_throws() {
//        Order o = orderWithStatus(OrderStatus.CREATED);
//        when(orders.findById(5L)).thenReturn(Optional.of(o));
//
//        assertThatThrownBy(() -> service.scheduleDelivery(5L, LocalDate.now().plusDays(1), List.of("VIN")))
//                .isInstanceOf(BusinessRuleExceptions.class)
//                .hasMessageContaining("Only APPROVED");
//    }
//
//    @Test
//    void scheduleDelivery_alreadyHasDelivery_throws() {
//        Order o = orderWithStatus(OrderStatus.APPROVED);
//        o.setDelivery(new Delivery());
//        when(orders.findById(6L)).thenReturn(Optional.of(o));
//
//        assertThatThrownBy(() -> service.scheduleDelivery(6L, LocalDate.now().plusDays(1), List.of("VIN")))
//                .isInstanceOf(BusinessRuleExceptions.class)
//                .hasMessageContaining("already has a scheduled delivery");
//    }
//
//    @Test
//    void scheduleDelivery_pastDate_throws() {
//        Order o = orderWithStatus(OrderStatus.APPROVED);
//        when(orders.findById(7L)).thenReturn(Optional.of(o));
//
//        assertThatThrownBy(() -> service.scheduleDelivery(7L, LocalDate.now().minusDays(1), List.of("VIN")))
//                .isInstanceOf(BusinessRuleExceptions.class)
//                .hasMessageContaining("today or later");
//    }
//
//    @Test
//    void scheduleDelivery_missingTrucksInDb_throws() {
//        Order o = orderWithStatus(OrderStatus.APPROVED);
//        o.setOrderNumber(13L);
//        when(orders.findById(13L)).thenReturn(Optional.of(o));
//
//        LocalDate date = nextWeekdayFrom(LocalDate.now().plusDays(1));
//
//        List<String> requestVins = List.of("VIN-OK", "VIN-MISSING");
//        when(trucks.findAllByVinIn(any())).thenReturn(
//                List.of(truck("VIN-OK",  true, 100L))
//        );
//
//        assertThatThrownBy(() -> service.scheduleDelivery(13L, date, requestVins))
//                .isInstanceOf(BusinessRuleExceptions.class)
//                .hasMessageContaining("Trucks not found");
//    }
//
//    @Test
//    void scheduleDelivery_truckBooked_throws() {
//        Order o = orderWithStatus(OrderStatus.APPROVED);
//        o.setOrderNumber(10L);
//        o.getItems().add(oi(10L, newItem(1L, "X", 10L, 5L, new BigDecimal("1")), 1L, 5L));
//        when(orders.findById(10L)).thenReturn(Optional.of(o));
//
//        LocalDate date = nextWeekdayFrom(LocalDate.now().plusDays(1));
//
//        when(trucks.findAllByVinIn(any())).thenReturn(List.of(truck("VIN-B", true, 100L)));
//        when(deliveries.isTruckBooked(eq(date), eq("VIN-B"))).thenReturn(true);
//
//        assertThatThrownBy(() -> service.scheduleDelivery(10L, date, List.of("VIN-B")))
//                .isInstanceOf(BusinessRuleExceptions.class)
//                .hasMessageContaining("already booked");
//    }
//
//    @Test
//    void scheduleDelivery_capacityTooLow_throws() {
//        Order o = orderWithStatus(OrderStatus.APPROVED);
//        o.setOrderNumber(11L);
//        Item it = newItem(1L, "X", 100L, 10L, new BigDecimal("1"));
//        o.getItems().add(oi(11L, it, 2L, null));
//        when(orders.findById(11L)).thenReturn(Optional.of(o));
//
//        when(trucks.findAllByVinIn(any())).thenReturn(List.of(truck("VIN-C", true, 10L)));
//        when(deliveries.isTruckBooked(any(), any())).thenReturn(false);
//
//        LocalDate date = nextWeekdayFrom(LocalDate.now().plusDays(1));
//
//        assertThatThrownBy(() -> service.scheduleDelivery(11L, date, List.of("VIN-C")))
//                .isInstanceOf(BusinessRuleExceptions.class)
//                .hasMessageContaining("capacity");
//    }
//
//    @Test
//    void scheduleDelivery_insufficientStock_throws() {
//        Order o = orderWithStatus(OrderStatus.APPROVED);
//        o.setOrderNumber(12L);
//        Item it = newItem(2L, "Y", 1L, 5L, new BigDecimal("1"));
//        o.getItems().add(oi(12L, it, 2L, 10L));
//
//        when(orders.findById(12L)).thenReturn(Optional.of(o));
//        LocalDate date = nextWeekdayFrom(LocalDate.now().plusDays(1));
//
//        when(trucks.findAllByVinIn(any())).thenReturn(List.of(truck("VIN-D", true, 100L)));
//        when(deliveries.isTruckBooked(eq(date), eq("VIN-D"))).thenReturn(false);
//
//        assertThatThrownBy(() -> service.scheduleDelivery(12L, date, List.of("VIN-D")))
//                .isInstanceOf(BusinessRuleExceptions.class)
//                .hasMessageContaining("Insufficient stock");
//    }
//
//    @Test
//    void getAvailableDaysForDelivery_notApproved_throws() {
//        Order o = orderWithStatus(OrderStatus.CREATED);
//        when(orders.findById(1L)).thenReturn(Optional.of(o));
//
//        assertThatThrownBy(() -> service.getAvailableDaysForDelivery(1L, 5))
//                .isInstanceOf(BusinessRuleExceptions.class)
//                .hasMessageContaining("only for APPROVED");
//    }
//
//    @Test
//    void getAvailableDaysForDelivery_noActiveTrucks_returnsEmpty() {
//        Order o = orderWithStatus(OrderStatus.APPROVED);
//        when(orders.findById(2L)).thenReturn(Optional.of(o));
//        when(trucks.findAllByActiveTrue()).thenReturn(List.of());
//
//        var res = service.getAvailableDaysForDelivery(2L, 5);
//        assertThat(res.availableDays()).isEmpty();
//    }
//
//    @Test
//    void getAvailableDaysForDelivery_capacityLessThanOrderVolume_returnsEmpty() {
//        Order o = orderWithStatus(OrderStatus.APPROVED);
//        Item it = newItem(1L, "X", 100L, 50L, new BigDecimal("1"));
//        o.getItems().add(oi(1L, it, 2L, null));
//        when(orders.findById(3L)).thenReturn(Optional.of(o));
//
//        when(trucks.findAllByActiveTrue()).thenReturn(List.of(truck("A", true, 30L), truck("B", true, 30L)));
//
//        var res = service.getAvailableDaysForDelivery(3L, 7);
//        assertThat(res.availableDays()).isEmpty();
//    }
}
