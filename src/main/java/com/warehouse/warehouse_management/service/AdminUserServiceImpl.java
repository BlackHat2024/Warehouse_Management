package com.warehouse.warehouse_management.service;

import com.warehouse.warehouse_management.dto.CreateUserRequest;
import com.warehouse.warehouse_management.dto.UpdatePasswordRequest;
import com.warehouse.warehouse_management.dto.UpdateUserRequest;
import com.warehouse.warehouse_management.dto.UserDto;
import com.warehouse.warehouse_management.entity.Role;
import com.warehouse.warehouse_management.entity.User;
import com.warehouse.warehouse_management.exceptions.BusinessRuleExceptions;
import com.warehouse.warehouse_management.mapper.UserMapper;
import com.warehouse.warehouse_management.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository users;
    private final UserMapper mapper;
    private final PasswordEncoder encoder;

    @Override
    public UserDto createUser(CreateUserRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new BusinessRuleExceptions("Email already in use");
        }
        User u = mapper.fromCreate(req);
        u.setPassword(encoder.encode(req.password()));
        return mapper.toDto(users.save(u));
    }

    @Override
    @Transactional()
    public UserDto getUser(Long id) {
        return mapper.toDto(users.findById(id)
                .orElseThrow(() -> new BusinessRuleExceptions("User not found")));
    }

    @Override
    @Transactional()
    public List<UserDto> listUsers(Role role) {
        List<User> list = (role == null) ? users.findAll() : users.findByRole(role);
        return list.stream().map(mapper::toDto).toList();
    }
    @Override
    public UserDto updateUser(Long id, UpdateUserRequest req) {
        User u = users.findById(id).orElseThrow(() -> new BusinessRuleExceptions("User not found"));
        if (req.email() != null && !req.email().equalsIgnoreCase(u.getEmail())
                && users.existsByEmailIgnoreCase(req.email())) {
            throw new BusinessRuleExceptions("Email already in use");
        }
        mapper.update(u, req);
        return mapper.toDto(users.save(u));
    }


    @Override
    public void deleteUser(Long id) {
        if (!users.existsById(id)) throw new BusinessRuleExceptions("User not found");
        users.deleteById(id);
    }
}
