package com.kokobae_bakery.kokobae_bakery.controller;

import com.kokobae_bakery.kokobae_bakery.model.Cart;
import com.kokobae_bakery.kokobae_bakery.model.Order;
import com.kokobae_bakery.kokobae_bakery.model.OrderItem;
import com.kokobae_bakery.kokobae_bakery.model.Product;
import com.kokobae_bakery.kokobae_bakery.model.User;
import com.kokobae_bakery.kokobae_bakery.repository.CartRepository;
import com.kokobae_bakery.kokobae_bakery.repository.OrderRepository;
import com.kokobae_bakery.kokobae_bakery.repository.ProductRepository;
import com.kokobae_bakery.kokobae_bakery.repository.UserRepository;
import com.kokobae_bakery.kokobae_bakery.service.NotificationService;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private NotificationService notificationService;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @Value("${delivery.allowed.pincodes:500094,500047}")
    private String allowedPincodesString;

    private List<String> getAllowedPincodes() {
        if (allowedPincodesString == null || allowedPincodesString.isEmpty()) {
            return Arrays.asList("500094", "500047");
        }
        return Arrays.asList(allowedPincodesString.split(","));
    }

    // Step 1: Prepare Razorpay order WITHOUT saving to DB
    // Called by frontend when user chooses ONLINE payment
    @PostMapping("/prepare")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> preparePayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        try {
            String userId = userDetails.getUsername();
            String deliveryType = body.get("deliveryType");
            String deliveryAddress = body.get("deliveryAddress");
            String deliveryPincode = body.get("deliveryPincode");

            // 1. Validate pincode if DELIVERY
            if ("DELIVERY".equals(deliveryType)) {
                if (deliveryPincode == null || deliveryPincode.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Pincode is required for delivery orders"));
                }
                List<String> allowedPincodes = getAllowedPincodes();
                if (!allowedPincodes.contains(deliveryPincode)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error",
                                    "Sorry, we currently deliver only to pincodes " +
                                            String.join(" and ", allowedPincodes)));
                }
                if (deliveryAddress == null || deliveryAddress.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Delivery address is required for delivery orders"));
                }
            }

            // 2. Get cart and calculate total server-side (do NOT trust frontend)
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Cart not found"));

            if (cart.getItems().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cannot place order with empty cart"));
            }

            double totalAmount = 0.0;
            for (var cartItem : cart.getItems()) {
                Product product = productRepository.findById(cartItem.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + cartItem.getProductId()));
                totalAmount += product.getPrice() * cartItem.getQuantity();
            }

            int amountPaise = (int) (totalAmount * 100);

            // 3. Get user details
            User user = userRepository.findByPhone(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 4. Create Razorpay order with notes (do NOT save to DB yet)
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject options = new JSONObject();
            options.put("amount", amountPaise);
            options.put("currency", "INR");
            options.put("payment_capture", 1);

            // Store checkout details in notes for webhook retrieval
            JSONObject notes = new JSONObject();
            notes.put("deliveryType", deliveryType);
            notes.put("deliveryAddress", deliveryAddress);
            notes.put("deliveryPincode", deliveryPincode);
            notes.put("userId", userId);
            notes.put("paymentMethod", "ONLINE");
            options.put("notes", notes);

            com.razorpay.Order rzpOrder = client.orders.create(options);

            return ResponseEntity.ok(Map.of(
                    "razorpayOrderId", rzpOrder.get("id").toString(),
                    "amount", amountPaise,
                    "currency", "INR",
                    "keyId", keyId,
                    "customerName", user.getFullName() != null ? user.getFullName() : user.getPhone(),
                    "customerPhone", user.getPhone(),
                    "checkoutDetails", Map.of(
                            "deliveryType", deliveryType,
                            "deliveryAddress", deliveryAddress,
                            "deliveryPincode", deliveryPincode
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Step 2: Webhook - Razorpay calls this after payment
    // Creates order ONLY after successful payment
    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        try {
            boolean valid = Utils.verifyWebhookSignature(
                    payload, signature, webhookSecret);

            if (!valid) {
                return ResponseEntity.status(400).body("Invalid signature");
            }

            JSONObject event = new JSONObject(payload);
            String eventType = event.getString("event");

            if ("payment.captured".equals(eventType)) {
                JSONObject paymentEntity = event
                        .getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                JSONObject notes = paymentEntity.optJSONObject("notes");
                if (notes == null) {
                    return ResponseEntity.ok("ok");
                }

                String userId = notes.optString("userId");
                String deliveryType = notes.optString("deliveryType");
                String deliveryAddress = notes.optString("deliveryAddress");
                String deliveryPincode = notes.optString("deliveryPincode");

                // Fetch user for name and phone
                User user = userRepository.findByPhone(userId).orElse(null);
                if (user == null) {
                    return ResponseEntity.ok("ok");
                }

                // Fetch cart
                Cart cart = cartRepository.findByUserId(userId).orElse(null);
                if (cart == null || cart.getItems().isEmpty()) {
                    return ResponseEntity.ok("ok");
                }

                // Build order items from cart
                List<OrderItem> orderItems = cart.getItems().stream()
                        .map(cartItem -> {
                            Product product = productRepository
                                    .findById(cartItem.getProductId()).orElse(null);
                            if (product == null) return null;
                            OrderItem oi = new OrderItem();
                            oi.setId(product.getId());
                            oi.setName(product.getName());
                            oi.setQuantity(cartItem.getQuantity());
                            oi.setPrice(product.getPrice());
                            return oi;
                        })
                        .filter(item -> item != null)
                        .collect(Collectors.toList());

                if (orderItems.isEmpty()) {
                    return ResponseEntity.ok("ok");
                }

                // Calculate total
                double total = orderItems.stream()
                        .mapToDouble(i -> i.getPrice() * i.getQuantity())
                        .sum();

                // Create and save order
                Order order = new Order();
                order.setUserId(userId);
                order.setOrderDate(Instant.now());
                order.setItems(orderItems);
                order.setTotalAmount(total);
                order.setPaymentMethod("ONLINE");
                order.setPaymentStatus("PAID");
                order.setStatus("PROCESSING");
                order.setDeliveryType(deliveryType);
                order.setDeliveryAddress(deliveryAddress);
                order.setDeliveryPincode(deliveryPincode);
                order.setCustomerName(user.getFullName());
                order.setCustomerPhone(user.getPhone());
                order.setRazorpayOrderId(paymentEntity.optString("order_id"));

                Order savedOrder = orderRepository.save(order);

                // Clear cart
                cart.setItems(new ArrayList<>());
                cartRepository.save(cart);

                // Send WhatsApp notifications
                notificationService.notifyNewOrder(savedOrder);
            }

            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // Legacy endpoint - kept for backward compatibility with existing orders
    @PostMapping("/create-razorpay-order")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createRazorpayOrder(
            @RequestBody Map<String, String> body) {
        try {
            String ourOrderId = body.get("orderId");
            Order order = orderRepository.findById(ourOrderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            int amountPaise = (int) (order.getTotalAmount() * 100);

            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject options = new JSONObject();
            options.put("amount", amountPaise);
            options.put("currency", "INR");
            options.put("receipt", ourOrderId);
            options.put("payment_capture", 1);

            com.razorpay.Order rzpOrder = client.orders.create(options);

            order.setRazorpayOrderId(rzpOrder.get("id"));
            orderRepository.save(order);

            return ResponseEntity.ok(Map.of(
                    "razorpayOrderId", rzpOrder.get("id").toString(),
                    "amount", amountPaise,
                    "currency", "INR",
                    "keyId", keyId,
                    "customerName", order.getCustomerName(),
                    "customerPhone", order.getCustomerPhone()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // COD notification - called when COD order is placed
    @PostMapping("/notify-cod")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> notifyCod(
            @RequestBody Map<String, String> body) {
        String orderId = body.get("orderId");
        orderRepository.findById(orderId).ifPresent(order -> {
            notificationService.notifyNewOrder(order);
        });
        return ResponseEntity.ok("ok");
    }
}
