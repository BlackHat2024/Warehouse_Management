package com.warehouse.warehouse_management.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;


    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "delivery_trucks",
            joinColumns = @JoinColumn(name = "delivery_id"),
            inverseJoinColumns = @JoinColumn(name = "truck_id",
                    referencedColumnName = "vin")
    )
    private Set<Truck> trucks = new HashSet<>();


    public void addTruck(Truck truck) {
        trucks.add(truck);
        truck.getDeliveries().add(this);
    }
//    public void removeTruck(Truck truck) {
//        trucks.remove(truck);
//        truck.getDeliveries().remove(this);
//    }

}