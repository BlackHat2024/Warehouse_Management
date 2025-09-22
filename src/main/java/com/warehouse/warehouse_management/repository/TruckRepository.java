package com.warehouse.warehouse_management.repository;

import com.warehouse.warehouse_management.entity.Truck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TruckRepository extends JpaRepository<Truck, String> {
    List<Truck> findAllByActiveTrue();
    List<Truck> findAllByVinIn(Iterable<String> vins);

}
