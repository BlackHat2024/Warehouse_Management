package com.warehouse.warehouse_management.controller;

import com.warehouse.warehouse_management.config.JwtConfig;
import com.warehouse.warehouse_management.dto.JwtResponse;
import com.warehouse.warehouse_management.dto.LoginRequest;
import com.warehouse.warehouse_management.dto.UpdatePasswordRequest;
import com.warehouse.warehouse_management.dto.UserDto;
import com.warehouse.warehouse_management.entity.User;
import com.warehouse.warehouse_management.exceptions.BusinessRuleExceptions;
import com.warehouse.warehouse_management.mapper.UserMapper;
import com.warehouse.warehouse_management.repository.UserRepository;
import com.warehouse.warehouse_management.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @PostMapping("/login")
    @Operation(summary = "LogIn")
    public ResponseEntity<JwtResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        var user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        var accessToken = jwtService.generateAccessToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        var cookie = new Cookie("refreshToken", refreshToken.toString());
        cookie.setHttpOnly(true);
        cookie.setPath("/auth/refresh");
        cookie.setMaxAge(jwtConfig.getRefreshTokenExpiration());
        cookie.setSecure(false);
        response.addCookie(cookie);

        return ResponseEntity.ok(new JwtResponse(accessToken.toString()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Token")
    public ResponseEntity<JwtResponse> refresh(@CookieValue(value = "refreshToken") String refreshToken) {
        if (refreshToken == null) {
            System.out.println("No refreshToken cookie on request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var jwt = jwtService.parseToken(refreshToken);
        if (jwt==null || jwt.isExpired()) {
            System.out.println("Invalid/expired refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userRepository.findById(jwt.getUserId()).orElseThrow();
        var accessToken = jwtService.generateAccessToken(user);
        return ResponseEntity.ok(new JwtResponse(accessToken.toString()));
    }

    @PatchMapping("/password")
    @Operation(summary = "Change Password")
    @Transactional
    public ResponseEntity<Void> changeMyPassword(@AuthenticationPrincipal Long userId,
                                                 @Valid @RequestBody UpdatePasswordRequest req) {
        User user = users.findById(userId)
                .orElseThrow(() -> new BusinessRuleExceptions("User not found"));

        if (!encoder.matches(req.currentPassword(), user.getPassword())) {
            throw new BusinessRuleExceptions("Current password is incorrect");
        }
        if (encoder.matches(req.newPassword(), user.getPassword())) {
            throw new BusinessRuleExceptions("New password must be different from the current one");
        }

        user.setPassword(encoder.encode(req.newPassword()));
        users.save(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = (Long) authentication.getPrincipal();


        var user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        var userDto = userMapper.toDto(user);

        return ResponseEntity.ok(userDto);
    }


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Void> handleBadCredentialsException() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
