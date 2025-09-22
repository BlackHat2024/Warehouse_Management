package com.warehouse.warehouse_management.mapper;

import com.warehouse.warehouse_management.dto.TruckDto;
import com.warehouse.warehouse_management.entity.Truck;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface TruckMapper {
    TruckDto toDto(Truck t);
    Truck toEntity(TruckDto req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget Truck t, TruckDto req);
}