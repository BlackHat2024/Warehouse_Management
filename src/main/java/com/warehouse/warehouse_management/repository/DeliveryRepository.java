package com.warehouse.warehouse_management.repository;

import com.warehouse.warehouse_management.entity.Delivery;
import com.warehouse.warehouse_management.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByScheduledDate(LocalDate date);
    @Query("""
        select (count(d) > 0)
        from Delivery d
        join d.trucks t
        where d.scheduledDate = :date and t.vin = :vin
    """)
    boolean isTruckBooked(@Param("date") LocalDate date, @Param("vin") String vin);
    List<Delivery> findByScheduledDateLessThanEqual(LocalDate date);
}