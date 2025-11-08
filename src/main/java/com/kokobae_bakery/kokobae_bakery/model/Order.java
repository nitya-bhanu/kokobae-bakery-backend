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
    private String paymentMethod; // e.g., "COD", "ONLINE", "UPI"
    private String status; // e.g., "PENDING", "COMPLETED", "CANCELLED"
}
