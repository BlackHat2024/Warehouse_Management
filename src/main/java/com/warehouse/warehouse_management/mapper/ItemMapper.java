package com.warehouse.warehouse_management.mapper;

import com.warehouse.warehouse_management.dto.ItemDto;
import com.warehouse.warehouse_management.entity.Item;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ItemMapper {
    @Mapping(target = "itemId",   source = "id")
    @Mapping(target = "itemName", source = "name")
    @Mapping(target = "price",    source = "unitPrice")
    @Mapping(target = "volume",   source = "packageVolume")
    ItemDto toDto(Item item);

    @Mapping(target = "id",            source = "itemId")
    @Mapping(target = "name",          source = "itemName")
    @Mapping(target = "unitPrice",     source = "price")
    @Mapping(target = "packageVolume", source = "volume")
    Item toEntity(ItemDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "name",          source = "itemName")
    @Mapping(target = "unitPrice",     source = "price")
    @Mapping(target = "packageVolume", source = "volume")
    void update(@MappingTarget Item entity, ItemDto dto);
}
