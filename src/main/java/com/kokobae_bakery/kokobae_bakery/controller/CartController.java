package com.kokobae_bakery.kokobae_bakery.controller;

import com.kokobae_bakery.kokobae_bakery.dto.CartItemDto;
import com.kokobae_bakery.kokobae_bakery.model.Cart;
import com.kokobae_bakery.kokobae_bakery.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    private String getUserId(Principal principal) {
        // In a real application, you would get the user ID from the authenticated principal
        // For now, let's assume the username is the userId for simplicity
        return principal.getName();
    }

    @GetMapping
    public ResponseEntity<Cart> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername(); // Assuming username is userId for now
        return cartService.getCartByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/add")
    public ResponseEntity<Cart> addItemToCart(@AuthenticationPrincipal UserDetails userDetails, @RequestBody CartItemDto cartItemDto) {
        String userId = userDetails.getUsername();
        try {
            Cart updatedCart = cartService.addItemToCart(userId, cartItemDto.getProductId(), cartItemDto.getQuantity());
            return ResponseEntity.ok(updatedCart);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null); // Or a more specific error response
        }
    }

    @PutMapping("/update")
    public ResponseEntity<Cart> updateCartItemQuantity(@AuthenticationPrincipal UserDetails userDetails, @RequestBody CartItemDto cartItemDto) {
        String userId = userDetails.getUsername();
        try {
            Cart updatedCart = cartService.updateCartItemQuantity(userId, cartItemDto.getProductId(), cartItemDto.getQuantity());
            return ResponseEntity.ok(updatedCart);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<Cart> removeCartItem(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String productId) {
        String userId = userDetails.getUsername();
        try {
            Cart updatedCart = cartService.removeCartItem(userId, productId);
            return ResponseEntity.ok(updatedCart);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails.getUsername();
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
