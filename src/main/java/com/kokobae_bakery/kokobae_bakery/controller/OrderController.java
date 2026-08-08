package com.kokobae_bakery.kokobae_bakery.controller;

import com.kokobae_bakery.kokobae_bakery.dto.OrderInitiateRequest;
import com.kokobae_bakery.kokobae_bakery.model.Order;
import com.kokobae_bakery.kokobae_bakery.service.EmailService;
import com.kokobae_bakery.kokobae_bakery.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final EmailService emailService;

    public OrderController(OrderService orderService, EmailService emailService) {
        this.orderService = orderService;
        this.emailService = emailService;
    }

    // Legacy endpoint - kept for backward compatibility
    @PostMapping("/place/{paymentMethod}")
    public ResponseEntity<Order> placeOrderLegacy(@AuthenticationPrincipal UserDetails userDetails,
                                                  @PathVariable String paymentMethod) {
        String userId = userDetails.getUsername();
        try {
            Order order = orderService.placeOrder(userId, paymentMethod);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/initiate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> initiateOrder(@AuthenticationPrincipal UserDetails userDetails,
                                           @Valid @RequestBody OrderInitiateRequest request) {
        String userId = userDetails.getUsername();

        // Reject ONLINE payments - use /api/payments/prepare instead
        if ("ONLINE".equals(request.getPaymentMethod())) {
            return ResponseEntity.status(400).body(
                    new ErrorResponse("Use /api/payments/prepare for online payments")
            );
        }

        try {
            Order order = orderService.initiateOrder(userId, request);
            // Send email notification for COD orders
            emailService.notifyOrderPlaced(order);
            return new ResponseEntity<>(order, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/my")
    public ResponseEntity<List<Order>> getMyOrders(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername();
        List<Order> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateOrderStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        try {
            return orderService.updateOrderStatus(id, status)
                    .map(order -> {
                        emailService.notifyStatusUpdate(order);
                        return ResponseEntity.ok(order);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    // Simple error response class
    private static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }
}
