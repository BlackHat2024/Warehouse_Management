package com.warehouse.warehouse_management.controller;

import com.warehouse.warehouse_management.dto.CreateUserRequest;
import com.warehouse.warehouse_management.dto.UpdatePasswordRequest;
import com.warehouse.warehouse_management.dto.UpdateUserRequest;
import com.warehouse.warehouse_management.dto.UserDto;
import com.warehouse.warehouse_management.entity.Role;
import com.warehouse.warehouse_management.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class SystemAdminController {

    private final AdminUserService admin;

    @PostMapping("/create")
    @Operation(summary = "Create new user")
    public UserDto createUser(@Valid @RequestBody CreateUserRequest req) {
        return admin.createUser(req);
    }

    @GetMapping("/{id}")
    @Operation(summary = "See specific user")
    public UserDto getUser(@PathVariable Long id) {
        return admin.getUser(id);
    }

    @GetMapping("/users")
    @Operation(summary = "See all user also filter by role")
    public List<UserDto> listUsers(@RequestParam(required = false) Role role) {
        return admin.listUsers(role);
    }

    @PatchMapping("/update/{id}")
    @Operation(summary = "Update user")
    public UserDto updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest req) {
        return admin.updateUser(id, req);
    }


    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete user")
    public void deleteUser(@PathVariable Long id) {
        admin.deleteUser(id);
    }
}
