package com.kokobae_bakery.kokobae_bakery.service;

import com.kokobae_bakery.kokobae_bakery.model.*;
import com.kokobae_bakery.kokobae_bakery.repository.OrderRepository;
import com.kokobae_bakery.kokobae_bakery.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, CartService cartService, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.productRepository = productRepository;
    }

    public Order placeOrder(String userId, String paymentMethod) {
        Cart cart = cartService.getCartByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user " + userId));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot place an order with an empty cart");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setOrderDate(Instant.now());
        order.setStatus("PENDING");
        order.setPaymentMethod(paymentMethod);

        List<OrderItem> orderItems = new ArrayList<>();
        AtomicReference<Double> totalAmount = new AtomicReference<>(0.0);

        cart.getItems().forEach(cartItem -> {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + cartItem.getProductId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setId(product.getId());
            orderItem.setName(product.getName());
            orderItem.setStock(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItems.add(orderItem);
            totalAmount.updateAndGet(v -> v + (product.getPrice() * cartItem.getQuantity()));
        });

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount.get());

        Order savedOrder = orderRepository.save(order);
        cartService.clearCart(userId); // Clear the cart after placing the order
        return savedOrder;
    }

    public List<Order> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> updateOrderStatus(String orderId, String status) {
        return orderRepository.findById(orderId).map(order -> {
            order.setStatus(status);
            return orderRepository.save(order);
        });
    }
}
