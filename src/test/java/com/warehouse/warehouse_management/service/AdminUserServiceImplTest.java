package com.warehouse.warehouse_management.service;

import com.warehouse.warehouse_management.dto.CreateUserRequest;
import com.warehouse.warehouse_management.dto.UpdateUserRequest;
import com.warehouse.warehouse_management.dto.UserDto;
import com.warehouse.warehouse_management.entity.Role;
import com.warehouse.warehouse_management.entity.User;
import com.warehouse.warehouse_management.exceptions.BusinessRuleExceptions;
import com.warehouse.warehouse_management.mapper.UserMapper;
import com.warehouse.warehouse_management.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock private UserRepository users;
    @Mock private UserMapper mapper;
    @Mock private PasswordEncoder encoder;

    @InjectMocks
    private AdminUserServiceImpl service;

    @Test
    void createUser_success_encodesPassword_andReturnsDto() {
        var req = new CreateUserRequest("Alice", "Brown", "alice@example.com", "Secret123!", Role.CLIENT);

        when(users.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);

        var toSave = new User();
        toSave.setEmail("alice@example.com");
        toSave.setRole(Role.CLIENT);
        when(mapper.fromCreate(req)).thenReturn(toSave);

        when(encoder.encode("Secret123!")).thenReturn("ENC-PW");

        var saved = new User();
        saved.setId(10L);
        saved.setEmail("alice@example.com");
        saved.setRole(Role.CLIENT);
        when(users.save(any(User.class))).thenReturn(saved);

        var dto = new UserDto(10L, "Alice", "Brown", "alice@example.com", Role.CLIENT);
        when(mapper.toDto(saved)).thenReturn(dto);

        var out = service.createUser(req);

        assertThat(out.id()).isEqualTo(10L);
        assertThat(out.email()).isEqualTo("alice@example.com");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("ENC-PW");

        verify(encoder).encode("Secret123!");
    }

    @Test
    void createUser_emailAlreadyUsed_throws() {
        var req = new CreateUserRequest("Alice", "Brown", "alice@example.com", "Secret123!", Role.CLIENT);
        when(users.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser(req))
                .isInstanceOf(BusinessRuleExceptions.class)
                .hasMessageContaining("Email already in use");

        verify(users, never()).save(any());
    }

    @Test
    void getUser_success() {
        var u = new User();
        u.setId(5L);
        u.setEmail("x@y.z");
        when(users.findById(5L)).thenReturn(Optional.of(u));

        var dto = new UserDto(5L, "X", "Y", "x@y.z", Role.CLIENT);
        when(mapper.toDto(u)).thenReturn(dto);

        var out = service.getUser(5L);
        assertThat(out.id()).isEqualTo(5L);
        assertThat(out.email()).isEqualTo("x@y.z");
    }

    @Test
    void getUser_notFound_throws() {
        when(users.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUser(123L))
                .isInstanceOf(BusinessRuleExceptions.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void listUsers_allRoles_mapsAll() {
        var u1 = new User(); u1.setId(1L); u1.setEmail("a@a.a");
        var u2 = new User(); u2.setId(2L); u2.setEmail("b@b.b");
        when(users.findAll()).thenReturn(List.of(u1, u2));

        var d1 = new UserDto(1L, "A", "A", "a@a.a", Role.CLIENT);
        var d2 = new UserDto(2L, "B", "B", "b@b.b", Role.WAREHOUSE_MANAGER);
        when(mapper.toDto(u1)).thenReturn(d1);
        when(mapper.toDto(u2)).thenReturn(d2);

        var out = service.listUsers(null);
        assertThat(out).extracting(UserDto::id).containsExactly(1L, 2L);
        verify(users).findAll();
        verify(users, never()).findByRole(any());
    }

    @Test
    void listUsers_byRole_callsFindByRole() {
        var u = new User(); u.setId(3L); u.setEmail("m@m.m"); u.setRole(Role.SYSTEM_ADMIN);
        when(users.findByRole(Role.SYSTEM_ADMIN)).thenReturn(List.of(u));
        when(mapper.toDto(u)).thenReturn(new UserDto(3L, "M", "M", "m@m.m", Role.SYSTEM_ADMIN));

        var out = service.listUsers(Role.SYSTEM_ADMIN);
        assertThat(out).hasSize(1);
        assertThat(out.get(0).role()).isEqualTo(Role.SYSTEM_ADMIN);

        verify(users).findByRole(Role.SYSTEM_ADMIN);
        verify(users, never()).findAll();
    }

    @Test
    void updateUser_emailUnchanged_updatesAndReturnsDto() {
        var existing = new User();
        existing.setId(7L);
        existing.setEmail("old@ex.com");
        when(users.findById(7L)).thenReturn(Optional.of(existing));

        var req = new UpdateUserRequest("NewName", "NewSurname", null, Role.CLIENT);

        doAnswer(inv -> null).when(mapper).update(existing, req);

        when(users.save(existing)).thenReturn(existing);
        when(mapper.toDto(existing)).thenReturn(new UserDto(7L, "NewName", "NewSurname", "old@ex.com", Role.CLIENT));

        var out = service.updateUser(7L, req);

        assertThat(out.id()).isEqualTo(7L);
        assertThat(out.email()).isEqualTo("old@ex.com");
        verify(mapper).update(existing, req);
        verify(users, never()).existsByEmailIgnoreCase(anyString());
    }

    @Test
    void updateUser_emailChangedAlreadyUsed_throws() {
        var existing = new User();
        existing.setId(8L);
        existing.setEmail("old@ex.com");
        when(users.findById(8L)).thenReturn(Optional.of(existing));

        var req = new UpdateUserRequest(null, null, "dup@ex.com", Role.CLIENT);
        when(users.existsByEmailIgnoreCase("dup@ex.com")).thenReturn(true);

        assertThatThrownBy(() -> service.updateUser(8L, req))
                .isInstanceOf(BusinessRuleExceptions.class)
                .hasMessageContaining("Email already in use");

        verify(users, never()).save(any());
    }

    @Test
    void deleteUser_success() {
        when(users.existsById(9L)).thenReturn(true);

        service.deleteUser(9L);

        verify(users).deleteById(9L);
    }

    @Test
    void deleteUser_notFound_throws() {
        when(users.existsById(11L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteUser(11L))
                .isInstanceOf(BusinessRuleExceptions.class)
                .hasMessageContaining("User not found");

        verify(users, never()).deleteById(anyLong());
    }
}
