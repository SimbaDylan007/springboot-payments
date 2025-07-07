package com.yourcompany.payments.service.auth;

import com.yourcompany.payments.dto.auth.LoginRequest;
import com.yourcompany.payments.dto.auth.RegisterRequest;
import com.yourcompany.payments.dto.auth.TokenResponse;
import com.yourcompany.payments.entity.User;
import com.yourcompany.payments.exception.UserAlreadyExistsException;
import com.yourcompany.payments.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.yourcompany.payments.service.auth.JwtService;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists.");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setHashedPassword(passwordEncoder.encode(request.password()));
        // Other fields like isActive have defaults in the entity

        return userRepository.save(user);
    }

    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );
        // If authentication is successful, user exists.
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));

        var jwtToken = jwtService.generateToken(user);
        return new TokenResponse(jwtToken, "Bearer");
    }
}