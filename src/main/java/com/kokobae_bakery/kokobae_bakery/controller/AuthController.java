package com.kokobae_bakery.kokobae_bakery.controller;

import com.kokobae_bakery.kokobae_bakery.dto.AuthRequest;
import com.kokobae_bakery.kokobae_bakery.dto.AuthResponse;
import com.kokobae_bakery.kokobae_bakery.dto.AuthResponseData;
import com.kokobae_bakery.kokobae_bakery.dto.LoginRequest;
import com.kokobae_bakery.kokobae_bakery.dto.RegisterRequest;
import com.kokobae_bakery.kokobae_bakery.model.User;
import com.kokobae_bakery.kokobae_bakery.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Legacy registration endpoint for backward compatibility
    @PostMapping("/register/legacy")
    public ResponseEntity<?> registerUserLegacy(@RequestBody AuthRequest authRequest) {
        try {
            User registeredUser = authService.registerUser(authRequest);
            return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            authService.registerUserWithPhone(registerRequest);
            // Generate JWT for the newly registered user
            AuthResponseData authData = authService.authenticateUserWithPhone(
                    new LoginRequest(registerRequest.getPhone(), registerRequest.getPassword())
            );
            AuthResponse response = new AuthResponse(authData.getToken(), authData.getId(),
                    authData.getFullName(), authData.getPhone(), authData.getRole());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // Legacy login endpoint for backward compatibility
    @PostMapping("/login/legacy")
    public ResponseEntity<AuthResponse> createAuthenticationTokenLegacy(@RequestBody AuthRequest authRequest) {
        AuthResponseData authData = authService.authenticateUser(authRequest);
        AuthResponse response = new AuthResponse(authData.getToken(), authData.getId(),
                authData.getFullName(), authData.getPhone(), authData.getRole());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> createAuthenticationToken(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponseData authData = authService.authenticateUserWithPhone(loginRequest);
        AuthResponse response = new AuthResponse(authData.getToken(), authData.getId(),
                authData.getFullName(), authData.getPhone(), authData.getRole());
        return ResponseEntity.ok(response);
    }
}
