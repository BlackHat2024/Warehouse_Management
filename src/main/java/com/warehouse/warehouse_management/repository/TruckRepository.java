package com.warehouse.warehouse_management.repository;

import com.warehouse.warehouse_management.entity.Truck;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TruckRepository extends JpaRepository<Truck, String> {
    List<Truck> findAllByActiveTrue();
    List<Truck> findAllByVinIn(Iterable<String> vins);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select t from Truck t
    where (:date > :today or t.active = true)
      and t.vin not in (select tr.vin from Delivery d join d.trucks tr
      where d.scheduledDate = :date)
""")
    List<Truck> findFreeTrucksOn(@Param("date") LocalDate date, @Param("today") LocalDate today);

}
