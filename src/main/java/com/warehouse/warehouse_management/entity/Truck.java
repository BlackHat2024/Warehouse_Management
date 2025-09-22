package com.warehouse.warehouse_management.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "trucks")
public class Truck {

    @Id
    @Column(name = "vin")
    private String vin;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "container_volume")
    private Long containerVolume;

    @Column(name = "active")
    private boolean active = true;

    @ManyToMany(mappedBy = "trucks", fetch = FetchType.LAZY)
    private Set<Delivery> deliveries = new HashSet<>();

}
