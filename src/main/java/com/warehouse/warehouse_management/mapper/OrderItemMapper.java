package com.warehouse.warehouse_management.mapper;

import com.warehouse.warehouse_management.dto.ItemDto;
import com.warehouse.warehouse_management.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {
    @Mapping(target = "itemId",source = "item.id")
    @Mapping(target = "itemName",source = "item.name")
    @Mapping(target = "quantity",source = "requestedQty")
    ItemDto toDto(OrderItem orderItem);
}
