package com.kokobae_bakery.kokobae_bakery.controller;

import com.kokobae_bakery.kokobae_bakery.dto.UpdateUserRequest;
import com.kokobae_bakery.kokobae_bakery.dto.UserProfileResponse;
import com.kokobae_bakery.kokobae_bakery.model.User;
import com.kokobae_bakery.kokobae_bakery.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        String phone = getCurrentUserPhone();
        if (phone == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        return authService.getCurrentUser(phone)
                .map(this::mapToProfileResponse)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateCurrentUser(@RequestBody UpdateUserRequest updateRequest) {
        String phone = getCurrentUserPhone();
        if (phone == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        return authService.getCurrentUser(phone)
                .map(user -> {
                    user.setDefaultAddress(updateRequest.getDefaultAddress());
                    user.setDefaultPincode(updateRequest.getDefaultPincode());
                    User updatedUser = authService.updateUser(user);
                    return ResponseEntity.ok(mapToProfileResponse(updatedUser));
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    private String getCurrentUserPhone() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        // The principal is the User object (or UserDetails username if not custom)
        // Since User.getUsername() returns phone, we can use getName()
        return authentication.getName();
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return new UserProfileResponse(
                user.getFullName(),
                user.getPhone(),
                user.getDefaultAddress(),
                user.getDefaultPincode()
        );
    }
}
