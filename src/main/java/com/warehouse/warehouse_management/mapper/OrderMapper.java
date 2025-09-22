package com.warehouse.warehouse_management.mapper;

import com.warehouse.warehouse_management.dto.OrderResponse;
import com.warehouse.warehouse_management.entity.Order;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {
    OrderResponse toDto(Order order);
}
