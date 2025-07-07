package com.yourcompany.payments.controller;

import com.yourcompany.payments.dto.UserDto;
import com.yourcompany.payments.dto.auth.LoginRequest;
import com.yourcompany.payments.dto.auth.RegisterRequest;
import com.yourcompany.payments.dto.auth.TokenResponse;
import com.yourcompany.payments.entity.User;
import com.yourcompany.payments.service.auth.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authService;

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        User registeredUser = authService.register(request);
        // Return a safe DTO, not the full entity with password hash
        UserDto userDto = new UserDto(
                registeredUser.getUserUuid(),
                registeredUser.getEmail(),
                registeredUser.getFullName(),
                registeredUser.isActive()
        );
        return ResponseEntity.status(201).body(userDto);
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}