# Kokobae Bakery - E-Commerce Backend

## Overview

Kokobae Bakery is a full-stack e-commerce platform for a home bakery business. This repository contains the **Spring Boot 3.2.5** backend API that powers the customer-facing storefront and admin dashboard.

## Project Architecture

### Tech Stack
- **Framework**: Spring Boot 3.2.5
- **Language**: Java 17
- **Database**: MongoDB Atlas (Cloud)
- **Security**: Spring Security + JWT
- **Payment**: Razorpay (India)
- **Email**: Brevo SMTP
- **Build Tool**: Maven
- **Deployment**: Docker, Render

### Domain Model

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│    User     │────▶│    Cart     │────▶│   Product   │
├─────────────┤     ├─────────────┤     ├─────────────┤
│ id          │     │ id          │     │ id          │
│ fullName    │     │ userId      │     │ name        │
│ email       │     │ items[]     │     │ price       │
│ phone (PK)  │     └─────────────┘     │ image       │
│ password    │           │             │ stock       │
│ role        │           ▼             └─────────────┘
│ address     │     ┌─────────────┐
│ pincode     │     │  CartItem   │
└─────────────┘     ├─────────────┤
       │            │ productId   │
       │            │ quantity    │
       ▼            └─────────────┘
┌─────────────┐
│    Order    │
├─────────────┤
│ id          │
│ userId      │
│ items[]     │
│ totalAmount │
│ paymentMethod (COD/ONLINE)
│ paymentStatus (PENDING/PAID/COD)
│ status (PROCESSING/SHIPPED/DELIVERED)
│ deliveryType (DELIVERY/PICKUP)
│ deliveryAddress
│ deliveryPincode
│ customerName
│ customerPhone
│ customerEmail
│ razorpayOrderId
└─────────────┘
```

## API Structure

### Public Endpoints (No Auth Required)
```
POST   /api/auth/register          - New user registration
POST   /api/auth/login             - User login
POST   /api/auth/register/legacy   - Legacy registration
POST   /api/auth/login/legacy      - Legacy login
GET    /api/products/**            - Product browsing
GET    /api/categories/**          - Category browsing
POST   /api/payments/webhook      - Razorpay webhook (signature verified)
POST   /api/messages               - Contact form submission
GET    /api/settings               - App settings
```

### Authenticated Endpoints (JWT Required)
```
Cart Management:
GET    /api/cart                   - Get user's cart
POST   /api/cart/items             - Add item to cart
DELETE /api/cart/items/{id}        - Remove item
DELETE /api/cart                    - Clear cart

Order Management:
POST   /api/orders/initiate        - Place COD order
POST   /api/orders/place/{method}  - Legacy order placement
GET    /api/orders/my              - Get my orders

Payment:
POST   /api/payments/prepare       - Create Razorpay order
POST   /api/payments/create-razorpay-order - Legacy endpoint

User Profile:
GET    /api/users/profile          - Get profile
PATCH  /api/users/profile          - Update profile
```

### Admin Endpoints (ROLE_ADMIN Required)
```
Orders:
GET    /api/orders/admin           - List all orders
PATCH  /api/orders/admin/{id}/status - Update order status

Products:
POST   /api/products               - Create product
PUT    /api/products/{id}         - Update product
DELETE /api/products/{id}         - Delete product

Categories:
POST   /api/categories             - Create category
PUT    /api/categories/{id}        - Update category
DELETE /api/categories/{id}       - Delete category

Messages:
GET    /api/messages               - List all messages
DELETE /api/messages/{id}         - Delete message

Settings:
PUT    /api/settings              - Update settings
```

## Authentication Flow

### JWT Implementation
```
1. Client POST /api/auth/login
   Body: { "phone": "7995978220", "password": "123456" }

2. Server validates credentials
   - Find user by phone
   - Verify BCrypt password
   - Generate JWT with claims:
     * subject: phone number
     * fullName: user's full name
     * role: CUSTOMER/ADMIN
     * issuedAt, expiration

3. Response: 
   {
     "token": "eyJhbGciOiJIUzI1NiIs...",
     "id": "user_id",
     "fullName": "John Doe",
     "phone": "7995978220",
     "role": "CUSTOMER"
   }

4. Client includes token in subsequent requests:
   Header: Authorization: Bearer <token>

5. JwtRequestFilter validates token on each request
```

### User Model
- `phone` is the primary key (replaces username)
- `username` field kept for backward compatibility (= phone)
- Unique indexes on `phone` and `email`
- Roles: CUSTOMER, ADMIN
- Default delivery address stored for faster checkout

## Order Lifecycle

### COD (Cash on Delivery) Flow
```
1. POST /api/orders/initiate
   Body: {
     "paymentMethod": "COD",
     "deliveryType": "DELIVERY",
     "deliveryAddress": "123 Main St",
     "deliveryPincode": "500094"
   }

2. Server:
   - Validate pincode against allowed list
   - Calculate total from cart (server-side, never trust frontend)
   - Create Order with status="PENDING_COD", paymentStatus="COD"
   - Clear cart
   - Send confirmation email via Brevo

3. Response: Order object with ID
```

### Online Payment Flow (Razorpay)
```
Step 1: Prepare Payment
POST /api/payments/prepare
├─ Validates pincode (if delivery)
├─ Calculates total from cart server-side
├─ Creates Razorpay order (NO local order yet)
├─ Returns: razorpayOrderId, amount, keyId, customer details
│
▼
Step 2: Frontend opens Razorpay checkout
User completes payment on Razorpay
│
▼
Step 3: Razorpay webhook calls
POST /api/payments/webhook
├─ Verifies webhook signature
├─ Extracts userId, delivery details from payment notes
├─ Fetches cart, builds order items
├─ Creates Order with status="PROCESSING", paymentStatus="PAID"
├─ Clears cart
├─ Sends confirmation email
└─ Returns "ok" (Razorpay expects 200)
```

### Order Status Transitions (Admin)
```
PENDING_COD ──▶ PROCESSING ──▶ SHIPPED ──▶ DELIVERED
     │              │            │
     ▼              ▼            ▼
 CANCELLED     READY_PICKUP ──▶ COLLECTED (for pickup orders)
```

Email notifications sent on status change:
- PROCESSING: "Your order is being prepared"
- SHIPPED: "Your order is on its way"
- READY_PICKUP: "Your order is ready for pickup"
- DELIVERED: "Order delivered!"
- COLLECTED: "Order collected!"
- CANCELLED: "Order cancelled"

## Payment Integration (Razorpay)

### Webflow
1. **Prepare Phase**: Create Razorpay order without saving to DB
   - Store checkout details in Razorpay `notes` field
   - This survives the payment flow

2. **Payment Capture**: Razorpay calls webhook
   - Signature verification mandatory
   - Extract all data from webhook payload
   - Create order only after confirmed payment

3. **Security Notes**:
   - Always calculate totals server-side
   - Never trust frontend-sent amounts
   - Webhook must return 200 quickly

### Environment Variables
```
RAZORPAY_KEY_ID=rzp_test_...
RAZORPAY_KEY_SECRET=...
RAZORPAY_WEBHOOK_SECRET=whsec_...
```

## Email Notifications (Brevo)

### Configuration
```
spring.mail.host=smtp-relay.brevo.com
spring.mail.port=587
spring.mail.username=${BREVO_EMAIL}
spring.mail.password=${BREVO_SMTP_KEY}

bakery.email.from=kokobae.bakery@gmail.com
bakery.email.fromName=Kokobae Bakery
```

### Email Templates
Rich HTML emails with:
- Kokobae brand styling (burgundy #8B1A1A, warm tones)
- Order summary with itemized list
- Delivery/pickup information
- Payment status
- WhatsApp contact link
- Reply-to: kokobae.bakery@gmail.com

### Triggers
- `notifyOrderPlaced()`: Called after COD order or successful online payment
- `notifyStatusUpdate()`: Called when admin updates order status

## Data Models

### User (MongoDB Document)
```java
@Document(collection = "users")
public class User implements UserDetails {
    @Id private String id;
    private String fullName;
    @Indexed(unique = true) private String email;
    @Indexed(unique = true) private String phone;  // Primary identifier
    private String username;  // = phone (backward compat)
    private String password;  // BCrypt encoded
    private String role;  // CUSTOMER, ADMIN
    private String defaultAddress;
    private String defaultPincode;
}
```

### Order (MongoDB Document)
```java
@Document(collection = "orders")
public class Order {
    @Id private String id;
    private String userId;  // Reference to User
    private Instant orderDate;
    private List<OrderItem> items;
    private Double totalAmount;
    private String paymentMethod;  // COD, ONLINE
    private String status;  // PROCESSING, SHIPPED, DELIVERED, etc.
    private String deliveryType;  // DELIVERY, PICKUP
    private String deliveryAddress;
    private String deliveryPincode;
    private String customerName;   // Denormalized
    private String customerPhone;  // Denormalized
    private String customerEmail;  // Denormalized
    private String paymentStatus;  // PENDING, PAID, COD
    private String razorpayOrderId;
}
```

### OrderItem (Embedded)
```java
public class OrderItem {
    private String id;        // Product ID
    private String name;      // Product name at time of order
    private Integer quantity;
    private Double price;     // Price at time of order
}
```

### Product (MongoDB Document)
```java
@Document(collection = "products")
public class Product {
    @Id private String id;
    private String name;
    private String description;
    private Double price;
    private String imageUrl;
    private Integer stock;
    private String categoryId;
    private Boolean available;
}
```

### Cart (MongoDB Document)
```java
@Document(collection = "carts")
public class Cart {
    @Id private String id;
    private String userId;
    private List<CartItem> items;
}
```

## Security Configuration

### CORS
```java
// Configurable via environment
CORS_ALLOWED_ORIGIN=https://kokobae-bakery.netlify.app,http://localhost:5173

// Falls back to allowing all origins if not set
// (useful for development)
```

### JWT Filter
- Intercepts all requests except permitted paths
- Extracts token from Authorization header
- Validates signature and expiration
- Sets SecurityContext with user details

### Protected Paths
```
Permit All:
- /api/auth/**
- /api/products/**
- /api/categories/**
- /api/payments/webhook
- /api/messages
- /api/settings

Authenticated:
- /api/cart/**
- /api/orders/**
- /api/payments/prepare
- /api/users/**

Admin Only:
- /api/admin/**
- /api/orders/admin/**
```

## Environment Variables

### Required for Production
```bash
# Database
MONGO_URI=mongodb+srv://user:pass@cluster.mongodb.net/kokobae

# JWT
JWT_SECRET=your-super-secret-jwt-key-min-256-bits

# CORS
CORS_ALLOWED_ORIGIN=https://your-frontend-domain.com

# Razorpay Payment
RAZORPAY_KEY_ID=rzp_test_... or rzp_live_...
RAZORPAY_KEY_SECRET=...
RAZORPAY_WEBHOOK_SECRET=whsec_...

# Email (Brevo)
BREVO_EMAIL=your-brevo-email@example.com
BREVO_SMTP_KEY=your-brevo-smtp-master-password

# Delivery
DELIVERY_ALLOWED_PINCODES=500094,500047
```

### Optional
```bash
# Server
PORT=8080 (Render sets this automatically)

# Debug
spring.profiles.active=dev
```

## Docker Deployment

### Build Image
```bash
docker build -t kokobae-bakery:latest .
```

### Run Container
```bash
docker run -p 8080:8080 \
  -e MONGO_URI="..." \
  -e JWT_SECRET="..." \
  -e RAZORPAY_KEY_ID="..." \
  -e RAZORPAY_KEY_SECRET="..." \
  -e BREVO_EMAIL="..." \
  -e BREVO_SMTP_KEY="..." \
  kokobae-bakery:latest
```

### Multi-Stage Dockerfile
- Stage 1: Maven build (maven:3.9.6-eclipse-temurin-17)
- Stage 2: Runtime (eclipse-temurin:17-jre)
- Result: ~200MB image vs ~1GB+ for full JDK

## Development Setup

### Prerequisites
- Java 17+
- Maven 3.9+
- MongoDB Atlas account (or local MongoDB)
- Razorpay test account
- Brevo account (free tier works)

### Run Locally
```bash
# 1. Clone repository
git clone https://github.com/nitya-bhanu/kokobae-bakery-backend.git
cd kokobae-bakery-backend/kokobae-bakery

# 2. Set environment variables (create .env or export)
export MONGO_URI="your-mongo-uri"
export JWT_SECRET="your-jwt-secret"
export RAZORPAY_KEY_ID="rzp_test_..."
export RAZORPAY_KEY_SECRET="..."

# 3. Run with Maven
./mvnw spring-boot:run

# 4. Or build and run JAR
./mvnw package -DskipTests
java -jar target/*.jar
```

### API Testing
```bash
# Health check
curl http://localhost:8080/api/settings

# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Test User",
    "email": "test@example.com",
    "phone": "7995978220",
    "password": "123456",
    "confirmPassword": "123456"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"7995978220","password":"123456"}'
```

## Frontend Integration

### React/Vue/Angular Pattern
```javascript
// 1. Login and store token
const response = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ phone, password })
});
const { token, fullName, role } = await response.json();
localStorage.setItem('token', token);

// 2. Use token for authenticated requests
const orders = await fetch('/api/orders/my', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
});

// 3. Razorpay integration
const { razorpayOrderId, amount, keyId } = await fetch('/api/payments/prepare', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}` },
  body: JSON.stringify({ deliveryType, deliveryAddress, deliveryPincode })
});

const options = {
  key: keyId,
  amount: amount,
  order_id: razorpayOrderId,
  handler: function(response) {
    // Payment successful, webhook will create order
    // Redirect to order confirmation page
  }
};
const rzp = new Razorpay(options);
rzp.open();
```

## Troubleshooting

### 403 Forbidden on Login
- Check CORS_ALLOWED_ORIGIN includes your frontend domain
- For development, leave CORS_ALLOWED_ORIGIN empty to allow all

### Emails Not Sending
- Verify BREVO_EMAIL and BREVO_SMTP_KEY in Render dashboard
- Check Brevo account has SMTP relay enabled
- Look for "Email failed" logs in Render

### Razorpay Webhook Failing
- Ensure RAZORPAY_WEBHOOK_SECRET matches Razorpay dashboard
- Webhook URL must be HTTPS in production
- Check webhook logs in Razorpay dashboard

### Database Connection Issues
- Verify MONGO_URI includes correct password
- Check IP allowlist in MongoDB Atlas (add Render's outbound IPs)
- Ensure database user has readWrite permissions

## Project Structure
```
kokobae-bakery/
├── src/main/java/com/kokobae_bakery/
│   ├── KokobaeBakeryApplication.java
│   ├── controller/
│   │   ├── AuthController.java      # Login, register
│   │   ├── CartController.java      # Cart operations
│   │   ├── CategoryController.java  # Category CRUD
│   │   ├── MessageController.java   # Contact form
│   │   ├── OrderController.java     # Order management
│   │   ├── PaymentController.java   # Razorpay integration
│   │   ├── ProductController.java   # Product CRUD
│   │   ├── SettingsController.java  # App settings
│   │   └── UserController.java      # User profile
│   ├── dto/                         # Data transfer objects
│   ├── model/                       # MongoDB entities
│   ├── repository/                  # Spring Data MongoDB
│   ├── security/                    # JWT, SecurityConfig
│   └── service/                     # Business logic
│       ├── AuthService.java
│       ├── CartService.java
│       ├── NotificationService.java # Email service
│       ├── OrderService.java
│       └── ProductService.java
├── src/main/resources/
│   └── application.properties       # Config (env vars)
├── Dockerfile                       # Multi-stage build
├── pom.xml                          # Maven dependencies
└── README.md                        # This file
```

## Contributing

This is a personal project for Kokobae Bakery. For major changes:
1. Create feature branch
2. Test thoroughly on staging
3. Deploy to production via Render

## License

MIT License - Created for Kokobae Bakery, Hyderabad.

## Contact

- **Bakery**: Sainikpuri, Hyderabad
- **WhatsApp**: +91 79959 78220
- **Email**: kokobae.bakery@gmail.com
- **Website**: https://kokobae.netlify.app
