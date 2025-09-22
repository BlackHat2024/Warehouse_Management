package com.warehouse.warehouse_management.repository;

import com.warehouse.warehouse_management.entity.Role;
import com.warehouse.warehouse_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByRole(Role role);
}
