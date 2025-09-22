package com.warehouse.warehouse_management.config;

import com.warehouse.warehouse_management.entity.OrderStatus;
import com.warehouse.warehouse_management.repository.DeliveryRepository;
import com.warehouse.warehouse_management.repository.OrderRepository;
import com.warehouse.warehouse_management.repository.TruckRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DeliveryStatusScheduler {

    private final DeliveryRepository deliveries;
    private final OrderRepository orders;
    private final TruckRepository truck;

    @Scheduled(cron = "0 28 23 * * *", zone = "Europe/Tirane")
    @Transactional
    public void markFulfilled() {
        LocalDate today = LocalDate.now();
        deliveries.findByScheduledDateLessThanEqual(today).forEach(d -> {
            var o = d.getOrder();
            if (o != null && o.getStatus() == OrderStatus.UNDER_DELIVERY) {
                o.setStatus(OrderStatus.FULFILLED);
                orders.save(o);
                if (d.getTrucks() != null) {
                    d.getTrucks().forEach(t -> {
                        if (!t.isActive()) {
                            t.setActive(true);
                            truck.save(t);
                        }
                    });
                }
            }

        });
    }
}

