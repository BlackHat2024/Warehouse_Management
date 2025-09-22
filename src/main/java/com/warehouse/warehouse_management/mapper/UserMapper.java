package com.warehouse.warehouse_management.mapper;

import com.warehouse.warehouse_management.dto.CreateUserRequest;
import com.warehouse.warehouse_management.dto.UpdateUserRequest;
import com.warehouse.warehouse_management.dto.UserDto;
import com.warehouse.warehouse_management.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orders", ignore = true)
    User fromCreate(CreateUserRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "orders", ignore = true)
    void update(@MappingTarget User entity, UpdateUserRequest req);
}
