package com.warehouse.warehouse_management.dto;

import com.warehouse.warehouse_management.entity.Role;

public record UpdateUserRequest(
        String name,
        String surname,
        String email,
        Role role
) {}
