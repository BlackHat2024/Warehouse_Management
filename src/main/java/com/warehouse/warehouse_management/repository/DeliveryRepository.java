package com.warehouse.warehouse_management.repository;

import com.warehouse.warehouse_management.entity.Delivery;
import com.warehouse.warehouse_management.entity.Order;
import com.warehouse.warehouse_management.entity.Priority;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Delivery d " +
            "join fetch d.trucks t " +
            "join fetch d.order o " +
            "where d.scheduledDate = :date and o.priority = :priority")
    List<Delivery> lockAllByDateAndPriority(@Param("date") LocalDate date,
                                            @Param("priority") Priority priority);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o left join fetch o.items i left join fetch i.item it " +
            "where o.orderNumber = :orderNumber")
    Optional<Order> findWithItemsForUpdate(@Param("orderNumber") Long orderNumber);

}