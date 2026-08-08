package com.kokobae_bakery.kokobae_bakery.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private String id;
    private String name;        // product name
    private Integer quantity;   // renamed from stock for clarity
    private Double price;

    // Helper method for EmailService
    public String getProductName() {
        return name;
    }
}
