package com.warehouse.warehouse_management.service;

import com.warehouse.warehouse_management.dto.CreateUserRequest;
import com.warehouse.warehouse_management.dto.UpdatePasswordRequest;
import com.warehouse.warehouse_management.dto.UpdateUserRequest;
import com.warehouse.warehouse_management.dto.UserDto;
import com.warehouse.warehouse_management.entity.Role;

import java.util.List;

public interface AdminUserService {
    UserDto createUser(CreateUserRequest req);
    UserDto getUser(Long id);
    List<UserDto> listUsers(Role role);
    UserDto updateUser(Long id, UpdateUserRequest req);
    void deleteUser(Long id);
}