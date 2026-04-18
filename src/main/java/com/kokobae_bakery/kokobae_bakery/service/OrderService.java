package com.kokobae_bakery.kokobae_bakery.service;

import com.kokobae_bakery.kokobae_bakery.dto.OrderInitiateRequest;
import com.kokobae_bakery.kokobae_bakery.model.*;
import com.kokobae_bakery.kokobae_bakery.repository.OrderRepository;
import com.kokobae_bakery.kokobae_bakery.repository.ProductRepository;
import com.kokobae_bakery.kokobae_bakery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Value("${delivery.allowed.pincodes}")
    private String allowedPincodesString;

    public OrderService(OrderRepository orderRepository, CartService cartService,
                        ProductRepository productRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    private List<String> getAllowedPincodes() {
        if (allowedPincodesString == null || allowedPincodesString.isEmpty()) {
            return Arrays.asList("500094", "500047");
        }
        return Arrays.asList(allowedPincodesString.split(","));
    }

    // Legacy method - kept for backward compatibility
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
            orderItem.setQuantity(cartItem.getQuantity());
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

    public Order initiateOrder(String userId, OrderInitiateRequest request) {
        // 1. Validate cart is not empty
        Cart cart = cartService.getCartByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user " + userId));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot place an order with an empty cart");
        }

        // 2. Validate pincode for DELIVERY orders
        if ("DELIVERY".equals(request.getDeliveryType())) {
            if (request.getDeliveryPincode() == null || request.getDeliveryPincode().isEmpty()) {
                throw new RuntimeException("Pincode is required for delivery orders");
            }
            List<String> allowedPincodes = getAllowedPincodes();
            if (!allowedPincodes.contains(request.getDeliveryPincode())) {
                throw new RuntimeException("Sorry, we currently deliver only to pincodes " +
                        String.join(" and ", allowedPincodes));
            }
            if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isEmpty()) {
                throw new RuntimeException("Delivery address is required for delivery orders");
            }
        }

        // 3. Get user details for denormalization
        User user = userRepository.findByPhone(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // 4. Create order items and calculate total
        List<OrderItem> orderItems = new ArrayList<>();
        AtomicReference<Double> totalAmount = new AtomicReference<>(0.0);

        cart.getItems().forEach(cartItem -> {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + cartItem.getProductId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setId(product.getId());
            orderItem.setName(product.getName());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItems.add(orderItem);
            totalAmount.updateAndGet(v -> v + (product.getPrice() * cartItem.getQuantity()));
        });

        // 5. Build order based on payment method
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderDate(Instant.now());
        order.setItems(orderItems);
        order.setTotalAmount(totalAmount.get());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setDeliveryType(request.getDeliveryType());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setDeliveryPincode(request.getDeliveryPincode());
        order.setCustomerName(user.getFullName());
        order.setCustomerPhone(user.getPhone());

        if ("COD".equals(request.getPaymentMethod())) {
            order.setStatus("PENDING_COD");
            order.setPaymentStatus("COD");
        } else { // ONLINE
            order.setStatus("PENDING_PAYMENT");
            order.setPaymentStatus("PENDING");
        }

        Order savedOrder = orderRepository.save(order);

        // Clear cart immediately for COD, keep for ONLINE (cleared after payment webhook)
        if ("COD".equals(request.getPaymentMethod())) {
            cartService.clearCart(userId);
        }

        return savedOrder;
    }

    public void confirmOnlinePayment(String orderId, String razorpayOrderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setRazorpayOrderId(razorpayOrderId);
        order.setPaymentStatus("PAID");
        order.setStatus("PROCESSING");

        orderRepository.save(order);

        // Clear cart after successful payment
        cartService.clearCart(order.getUserId());
    }

    public List<Order> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> updateOrderStatus(String orderId, String status) {
        // Validate status value
        List<String> validStatuses = Arrays.asList(
                "PENDING_COD", "PENDING_PAYMENT", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED", "READY_PICKUP", "COLLECTED"
        );
        if (!validStatuses.contains(status)) {
            throw new RuntimeException("Invalid status value. Valid values: " + String.join(", ", validStatuses));
        }

        return orderRepository.findById(orderId).map(order -> {
            // Handle cancellation - restore stock if needed
            if ("CANCELLED".equals(status) && !"CANCELLED".equals(order.getStatus())) {
                restoreStockForOrder(order);
            }

            order.setStatus(status);
            return orderRepository.save(order);
        });
    }

    private void restoreStockForOrder(Order order) {
        // Stock restoration logic - if products had inventory tracking
        // Currently products don't have stock field in OrderItem, so this is a placeholder
        // In a real system, you would increment product stock quantities here
        // For now, we just log that stock should be restored
        System.out.println("Stock restoration triggered for order: " + order.getId());
    }
}
