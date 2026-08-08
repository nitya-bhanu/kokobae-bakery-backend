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
import com.kokobae_bakery.kokobae_bakery.service.EmailService;
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
    private EmailService emailService;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @Value("${delivery.allowed.pincode.pattern:^[1-9][0-9]{5}$}")
    private String allowedPincodePattern;

    private boolean isPincodeAllowed(String pincode) {
        if (pincode == null || pincode.isEmpty()) {
            return false;
        }
        return pincode.matches(allowedPincodePattern);
    }

    // Step 1: Prepare Razorpay order WITHOUT saving to DB
    // Called by frontend when user chooses ONLINE payment
    @PostMapping("/prepare")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> preparePayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        try {
            if (keyId == null || keyId.isEmpty() || keySecret == null || keySecret.isEmpty()) {
                return ResponseEntity.status(500)
                        .body(Map.of("error", "Razorpay keys are not configured on the server"));
            }

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
                if (!isPincodeAllowed(deliveryPincode)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Please enter a valid 6-digit pincode"));
                }
                if (deliveryAddress == null || deliveryAddress.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Delivery address is required for delivery orders"));
                }
            }

            // 2. Get cart and calculate total server-side (do NOT trust frontend)
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId + ". Please add items to your cart first."));

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
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            // 4. Create Razorpay order with notes (do NOT save to DB yet)
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject options = new JSONObject();
            options.put("amount", amountPaise);
            options.put("currency", "INR");
            options.put("payment_capture", 1);

            // Store checkout details in notes for webhook retrieval
            JSONObject notes = new JSONObject();
            notes.put("deliveryType", deliveryType);
            notes.put("deliveryAddress", deliveryAddress != null ? deliveryAddress : "");
            notes.put("deliveryPincode", deliveryPincode != null ? deliveryPincode : "");
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
                            "deliveryAddress", deliveryAddress != null ? deliveryAddress : "",
                            "deliveryPincode", deliveryPincode != null ? deliveryPincode : ""
                    )
            ));
        } catch (Exception e) {
            e.printStackTrace(); // Log the stack trace for local debugging
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Internal Server Error during payment preparation"));
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
                System.err.println("[WEBHOOK] Invalid Razorpay Webhook Signature");
                return ResponseEntity.status(400).body("Invalid signature");
            }

            JSONObject event = new JSONObject(payload);
            String eventType = event.getString("event");

            if ("payment.captured".equals(eventType) || "order.paid".equals(eventType)) {
                JSONObject payloadObj = event.getJSONObject("payload");
                JSONObject notes = null;
                String rzpOrderId = null;

                // 1. Try to get data from Order entity (best source for notes)
                if (payloadObj.has("order")) {
                    JSONObject orderEntity = payloadObj.getJSONObject("order").getJSONObject("entity");
                    notes = orderEntity.optJSONObject("notes");
                    rzpOrderId = orderEntity.optString("id");
                }

                // 2. Try to get data from Payment entity
                if (payloadObj.has("payment")) {
                    JSONObject paymentEntity = payloadObj.getJSONObject("payment").getJSONObject("entity");
                    if (notes == null || notes.length() == 0) {
                        notes = paymentEntity.optJSONObject("notes");
                    }
                    if (rzpOrderId == null || rzpOrderId.isEmpty()) {
                        rzpOrderId = paymentEntity.optString("order_id");
                    }
                }

                // 3. Fallback: Fetch from Razorpay API if notes are still missing
                if ((notes == null || notes.length() == 0) && (rzpOrderId != null && !rzpOrderId.isEmpty())) {
                    System.out.println("Notes missing in webhook payload, fetching order from Razorpay: " + rzpOrderId);
                    RazorpayClient client = new RazorpayClient(keyId, keySecret);
                    com.razorpay.Order rzpOrder = client.orders.fetch(rzpOrderId);
                    if (rzpOrder != null) {
                        notes = rzpOrder.toJson().optJSONObject("notes");
                    }
                }

                if (notes == null || notes.length() == 0) {
                    System.err.println("[WEBHOOK] Could not find notes for Razorpay Order: " + rzpOrderId);
                    return ResponseEntity.ok("ok");
                }

                // 4. Check for duplicate orders
                if (rzpOrderId != null && !rzpOrderId.isEmpty()) {
                    if (orderRepository.existsByRazorpayOrderId(rzpOrderId)) {
                        System.out.println("[WEBHOOK] Duplicate order, skipping: " + rzpOrderId);
                        return ResponseEntity.ok("ok");
                    }
                }

                String userId = notes.optString("userId");
                String deliveryType = notes.optString("deliveryType");
                String deliveryAddress = notes.optString("deliveryAddress");
                String deliveryPincode = notes.optString("deliveryPincode");

                // Fallback 1: Use contact from payment entity if userId missing in notes
                if ((userId == null || userId.isEmpty()) && payloadObj.has("payment")) {
                    userId = payloadObj.getJSONObject("payment").getJSONObject("entity").optString("contact");
                    System.out.println("userId missing in notes, falling back to payment contact: " + userId);
                }

                if (userId == null || userId.isEmpty()) {
                    System.err.println("No userId in notes or contact in payment for Razorpay Order: " + rzpOrderId);
                    return ResponseEntity.ok("ok");
                }

                // Normalize phone number (handle +91 prefix)
                String normalizedUserId = userId;
                if (userId.startsWith("+91")) {
                    normalizedUserId = userId.substring(3);
                } else if (userId.length() > 10 && userId.startsWith("91")) {
                    normalizedUserId = userId.substring(2);
                }

                // Fetch user for name and phone
                User user = userRepository.findByPhone(normalizedUserId).orElse(null);
                if (user == null) {
                    System.err.println("[WEBHOOK] User not found: " + normalizedUserId);
                    return ResponseEntity.ok("ok");
                }

                // Fetch cart
                Cart cart = cartRepository.findByUserId(normalizedUserId).orElse(null);
                if (cart == null || cart.getItems().isEmpty()) {
                    System.err.println("[WEBHOOK] Cart empty for user: " + normalizedUserId);
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
                    System.err.println("[WEBHOOK] No valid items for user: " + normalizedUserId);
                    return ResponseEntity.ok("ok");
                }

                // Calculate total
                double total = orderItems.stream()
                        .mapToDouble(i -> i.getPrice() * i.getQuantity())
                        .sum();

                // Create and save order
                Order order = new Order();
                order.setUserId(normalizedUserId);
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
                order.setCustomerEmail(user.getEmail());
                order.setRazorpayOrderId(rzpOrderId);

                Order savedOrder = orderRepository.save(order);
                System.out.println("[WEBHOOK] Order created: " + savedOrder.getId() + " for user: " + normalizedUserId);

                // Clear cart
                cart.setItems(new ArrayList<>());
                cartRepository.save(cart);

                // Send email notification
                try {
                    emailService.notifyOrderPlaced(savedOrder);
                } catch (Exception e) {
                    System.err.println("[WEBHOOK] Email failed: " + e.getMessage());
                }
            }

            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            System.err.println("[WEBHOOK] Error: " + e.getMessage());
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
                    "customerName", order.getCustomerName() != null ? order.getCustomerName() : "",
                    "customerPhone", order.getCustomerPhone() != null ? order.getCustomerPhone() : ""
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Internal Server Error during payment creation"));
        }
    }

}
