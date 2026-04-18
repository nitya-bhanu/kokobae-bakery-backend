package com.kokobae_bakery.kokobae_bakery.service;

import com.kokobae_bakery.kokobae_bakery.dto.AuthRequest;
import com.kokobae_bakery.kokobae_bakery.dto.AuthResponseData;
import com.kokobae_bakery.kokobae_bakery.dto.LoginRequest;
import com.kokobae_bakery.kokobae_bakery.dto.RegisterRequest;
import com.kokobae_bakery.kokobae_bakery.model.User;
import com.kokobae_bakery.kokobae_bakery.repository.UserRepository;
import com.kokobae_bakery.kokobae_bakery.security.CustomUserDetailsService;
import com.kokobae_bakery.kokobae_bakery.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, AuthenticationManager authenticationManager, CustomUserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    // Legacy method for backward compatibility
    public User registerUser(AuthRequest authRequest) {
        if (userRepository.findByUsername(authRequest.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        User newUser = new User();
        newUser.setUsername(authRequest.getUsername());
        newUser.setPhone(authRequest.getUsername()); // For backward compat
        newUser.setPassword(passwordEncoder.encode(authRequest.getPassword()));
        newUser.setRole("CUSTOMER"); // Default role
        return userRepository.save(newUser);
    }

    public User registerUserWithPhone(RegisterRequest registerRequest) {
        // Validate confirm password
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        // Check if phone already registered
        if (userRepository.existsByPhone(registerRequest.getPhone())) {
            throw new RuntimeException("Phone number already registered");
        }

        User newUser = new User();
        newUser.setFullName(registerRequest.getFullName());
        newUser.setPhone(registerRequest.getPhone());
        newUser.setUsername(registerRequest.getPhone()); // Set username = phone for backward compat
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setRole("CUSTOMER"); // Default role

        return userRepository.save(newUser);
    }

    // Legacy method for backward compatibility
    public AuthResponseData authenticateUser(AuthRequest authRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        User user = (User) userDetails;
        return new AuthResponseData(token, user.getId(), user.getFullName(), user.getPhone(), user.getRole());
    }

    public AuthResponseData authenticateUserWithPhone(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getPhone(), loginRequest.getPassword())
        );
        final UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getPhone());
        String token = jwtUtil.generateToken(userDetails);

        User user = (User) userDetails;
        return new AuthResponseData(token, user.getId(), user.getFullName(), user.getPhone(), user.getRole());
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    public Optional<User> getCurrentUser(String phone) {
        return userRepository.findByPhone(phone);
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
