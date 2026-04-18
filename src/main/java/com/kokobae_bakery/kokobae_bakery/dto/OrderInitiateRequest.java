package com.kokobae_bakery.kokobae_bakery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderInitiateRequest {

    @NotBlank(message = "Delivery type is required")
    @Pattern(regexp = "^(DELIVERY|PICKUP)$", message = "Delivery type must be DELIVERY or PICKUP")
    private String deliveryType;

    private String deliveryAddress;  // required if DELIVERY

    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits")
    private String deliveryPincode;  // required if DELIVERY

    @NotBlank(message = "Payment method is required")
    @Pattern(regexp = "^(COD|ONLINE)$", message = "Payment method must be COD or ONLINE")
    private String paymentMethod;
}
