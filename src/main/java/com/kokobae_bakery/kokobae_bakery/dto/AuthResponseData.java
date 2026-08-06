package com.kokobae_bakery.kokobae_bakery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseData {
    private String token;
    private String id;
    private String fullName;
    private String phone;
    private String role;
    private String email;
}
