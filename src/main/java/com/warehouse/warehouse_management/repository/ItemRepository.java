package com.warehouse.warehouse_management.repository;

import com.warehouse.warehouse_management.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Item i where i.id = :id and i.quantity = 0")
    int deleteIfQuantityZero(@Param("id") Long id);
}
