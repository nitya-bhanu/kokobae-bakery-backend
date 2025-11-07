package com.kokobae_bakery.kokobae_bakery.service;

import com.kokobae_bakery.kokobae_bakery.dto.CartItemDto;
import com.kokobae_bakery.kokobae_bakery.model.Cart;
import com.kokobae_bakery.kokobae_bakery.model.CartItem;
import com.kokobae_bakery.kokobae_bakery.repository.CartRepository;
import com.kokobae_bakery.kokobae_bakery.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    public Optional<Cart> getCartByUserId(String userId) {
        return cartRepository.findByUserId(userId);
    }

    public Cart synchronizeCart(String userId, List<CartItemDto> desiredItems) {
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            return newCart;
        });

        List<CartItem> newCartItems = new ArrayList<>();
        if (desiredItems != null) {
            for (CartItemDto itemDto : desiredItems) {
                // Ensure product exists and quantity is valid before adding
                if (itemDto.getQuantity() > 0) {
                    productRepository.findById(itemDto.getProductId())
                            .orElseThrow(() -> new RuntimeException("Attempted to add non-existent product to cart: " + itemDto.getProductId()));
                    newCartItems.add(new CartItem(itemDto.getProductId(), itemDto.getQuantity()));
                }
            }
        }

        cart.setItems(newCartItems);
        return cartRepository.save(cart);
    }

    public Cart updateCartItemQuantity(String userId, String productId, int quantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user " + userId));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(quantity);
            if (quantity <= 0) {
                cart.getItems().remove(existingItem.get());
            }
        } else {
            throw new RuntimeException("Product not in cart");
        }
        return cartRepository.save(cart);
    }

    public Cart removeCartItem(String userId, String productId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user " + userId));

        boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        if (!removed) {
            throw new RuntimeException("Product not found in cart");
        }
        return cartRepository.save(cart);
    }

    public void clearCart(String userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.setItems(new ArrayList<>());
            cartRepository.save(cart);
        });
    }
}
