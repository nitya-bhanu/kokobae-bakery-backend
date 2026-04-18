package com.kokobae_bakery.kokobae_bakery.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {
    @Id
    private String id;
    private String userId; // reference to User's ID
    private Instant orderDate;
    private List<OrderItem> items;
    private Double totalAmount;

    // Existing fields - kept
    private String paymentMethod;  // COD, ONLINE
    private String status;         // PENDING_COD, PENDING_PAYMENT, PROCESSING, SHIPPED, DELIVERED, CANCELLED

    // New fields
    private String deliveryType;    // DELIVERY or PICKUP
    private String deliveryAddress; // null if PICKUP
    private String deliveryPincode; // null if PICKUP
    private String customerName;    // denormalized for admin view
    private String customerPhone;   // denormalized for admin view
    private String paymentStatus;   // PENDING, PAID, COD
    private String razorpayOrderId; // for online payments, filled later
}
