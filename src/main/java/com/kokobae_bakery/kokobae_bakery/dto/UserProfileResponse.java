package com.kokobae_bakery.kokobae_bakery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String fullName;
    private String phone;
    private String defaultAddress;
    private String defaultPincode;
}
