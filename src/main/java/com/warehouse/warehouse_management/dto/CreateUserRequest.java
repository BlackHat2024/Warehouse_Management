package com.warehouse.warehouse_management.dto;

import com.warehouse.warehouse_management.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank String name,
        String surname,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotNull Role role
) {}