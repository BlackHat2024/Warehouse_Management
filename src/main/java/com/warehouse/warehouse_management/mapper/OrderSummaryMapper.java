package com.warehouse.warehouse_management.mapper;

import com.warehouse.warehouse_management.dto.OrderSummaryResponse;
import com.warehouse.warehouse_management.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderSummaryMapper {
    @Mapping(target = "clientName", expression = "java(order.getClient().getName() + \" \" + order.getClient().getSurname())")
    OrderSummaryResponse toSummary(Order order);
}