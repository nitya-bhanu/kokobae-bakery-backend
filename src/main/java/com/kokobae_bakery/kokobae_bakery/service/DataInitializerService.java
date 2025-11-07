package com.kokobae_bakery.kokobae_bakery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.kokobae_bakery.kokobae_bakery.model.*;
import com.kokobae_bakery.kokobae_bakery.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class DataInitializerService implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final SettingsRepository settingsRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializerService(ProductRepository productRepository,
                                  CategoryRepository categoryRepository,
                                  UserRepository userRepository,
                                  MessageRepository messageRepository,
                                  SettingsRepository settingsRepository,
                                  ObjectMapper objectMapper,
                                  PasswordEncoder passwordEncoder) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.settingsRepository = settingsRepository;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() == 0 &&
                categoryRepository.count() == 0 &&
                userRepository.count() == 0 &&
                messageRepository.count() == 0 &&
                settingsRepository.count() == 0) {
            loadInitialData();
        }
    }

    private void loadInitialData() throws IOException {
        // Using a dummy JSON string for now, will replace with actual file reading
        String jsonData = """
                {
                  "products": [
                    {
                      "id": "1",
                      "name": "OG Fudge",
                      "price": 80,
                      "image": "../../../assets/images/Brownies/OG Fudge.png",
                      "dealSeller": true,
                      "outOfStock": false,
                      "category": "Brownies",
                      "bestSeller": false,
                      "visible": true,
                      "description": "Pure bliss. No extras, no rules - just chocolate doing its thing.",
                      "imgAltText": "Stack of three rich, fudgy chocolate brownies on a light background.",
                      "discount": 0,
                      "containsEgg": false,
                      "isPriceVisible": true
                    },
                    {
                      "id": "2",
                      "name": "Confetti Pop",
                      "price": 90,
                      "image": "../../../assets/images/Brownies/Confetti Pop.png",
                      "dealSeller": true,
                      "outOfStock": false,
                      "category": "Brownies",
                      "bestSeller": false,
                      "visible": true,
                      "description": "White chocolate glaze and sprinkles- your inner child just did a happy dance!",
                      "imgAltText": "Fudgy chocolate brownie topped with a generous layer of colorful rainbow sprinkles.",
                      "discount": 0,
                      "containsEgg": false,
                      "isPriceVisible": true
                    },
                    {
                      "id": "3",
                      "name": "Choco Chip",
                      "price": 100,
                      "image": "../../../assets/images/Brownies/Choco Chip.png",
                      "dealSeller": true,
                      "outOfStock": false,
                      "category": "Brownies",
                      "bestSeller": false,
                      "visible": true,
                      "description": "Unfiltered love for chocolate in its simplest form.",
                      "imgAltText": "Moist chocolate chip brownie with visible melted chocolate chips.",
                      "discount": 0,
                      "containsEgg": true,
                      "isPriceVisible": true
                    },
                    {
                      "id": "4",
                      "name": "Oreo Overload",
                      "price": 100,
                      "image": "../../../assets/images/Brownies/Oreo Overload.png",
                      "dealSeller": true,
                      "outOfStock": false,
                      "category": "Brownies",
                      "bestSeller": false,
                      "visible": true,
                      "description": "A timeless classic with a nutty crunch in every bite",
                      "imgAltText": "Rich brownie loaded with crushed Oreo pieces and a light drizzle of icing.",
                      "discount": 0,
                      "containsEgg": false,
                      "isPriceVisible": true
                    },
                    {
                      "id": "5",
                      "name": "Walnut Crunch",
                      "price": 100,
                      "image": "../../../assets/images/Brownies/Walnut Crunch.png",
                      "dealSeller": true,
                      "outOfStock": false,
                      "category": "Brownies",
                      "bestSeller": false,
                      "visible": true,
                      "description": "Chocolate brownie with crunchy walnut pieces baked into the top layer.",
                      "imgAltText": "Chocolate brownie with crunchy walnut pieces baked into the top layer.",
                      "discount": 0,
                      "containsEgg": true,
                      "isPriceVisible": true
                    },
                    {
                      "id": "6",
                      "name": "Nutella Craze",
                      "price": 100,
                      "image": "../../../assets/images/Brownies/Nutella Craze.png",
                      "dealSeller": true,
                      "outOfStock": false,
                      "category": "Brownies",
                      "bestSeller": false,
                      "visible": true,
                      "description": "A swirl of hazelnut heaven for that melt-in-mouth magic",
                      "imgAltText": "Fudgy brownie with a generous swirl of hazelnut Nutella baked into the center.",
                      "discount": 0,
                      "containsEgg": false,
                      "isPriceVisible": true
                    },
                    {
                      "id": "7",
                      "name": "Banana, Nutella & Walnut",
                      "price": 450,
                      "image": "../../../assets/images/Loaf Cakes/Banana Nutella and Walnut.png",
                      "dealSeller": true,
                      "outOfStock": false,
                      "category": "Loaf Cakes",
                      "bestSeller": false,
                      "visible": true,
                      "description": "Packed with ripe bananas, nutella and walnuts, it's a satisfying mouthful of joy",
                      "imgAltText": "Long slice of moist banana loaf cake with walnuts and Nutella swirl, light background.",
                      "discount": 0,
                      "containsEgg": false,
                      "isPriceVisible": true
                    },
                    {
                      "id": "8",
                      "name": "Vanilla & Almond",
                      "price": 450,
                      "image": "../../../assets/images/Loaf Cakes/Vanilla & Almond.png",
                      "dealSeller": true,
                      "outOfStock": false,
                      "category": "Loaf Cakes",
                      "bestSeller": false,
                      "visible": true,
                      "description": "A soft, fragrant vanilla base crowned with golden almonds that add warmth and balance to every bite",
                      "imgAltText": "Long slice of fragrant vanilla loaf cake topped with golden toasted almonds.",
                      "discount": 0,
                      "containsEgg": true,
                      "isPriceVisible": true
                    },
                    {
                      "id": "9",
                      "name": "Ragi Chocolate",
                      "price": 450,
                      "image": "../../../assets/images/Loaf Cakes/Ragi Chocolate.png",
                      "dealSeller": true,
                      "outOfStock": false,
                      "category": "Loaf Cakes",
                      "bestSeller": false,
                      "visible": true,
                      "description": "Every slice is a cozy hug mellow sweetness wrapped in the richness of cocoa",
                      "imgAltText": "Long slice of rich, dark Ragi chocolate loaf cake.",
                      "discount": 0,
                      "containsEgg": false,
                      "isPriceVisible": true
                    },
                    {
                      "id": "10",
                      "name": "Vanilla & Almond (Atta, Jaggery)",
                      "price": 450,
                      "image": "../../../assets/images/Loaf Cakes/Vanilla & Almond.png",
                      "dealSeller": true,
                      "outOfStock": false,
                      "category": "Loaf Cakes",
                      "bestSeller": false,
                      "visible": true,
                      "description": "Vanilla & almond, but refined flour & refined sugar free",
                      "imgAltText": "Long slice of Vanilla and Almond loaf made with whole wheat (atta) and jaggery.",
                      "discount": 0,
                      "containsEgg": false,
                      "isPriceVisible": true
                    },
                    {
                      "id": "11",
                      "name": "Coffee Walnut Crumble",
                      "price": 450,
                      "image": "../../../assets/images/Loaf Cakes/Coffee Walnut Crumble.png",
                      "dealSeller": true,
                      "outOfStock": false,
                      "category": "Loaf Cakes",
                      "bestSeller": false,
                      "visible": true,
                      "description": "The comforting aroma of coffee meets the gentle richness of walnuts a slice that feels like a slow morning",
                      "imgAltText": "Long slice of coffee-flavored loaf cake topped with a crunchy walnut crumble.",
                      "discount": 0,
                      "containsEgg": false,
                      "isPriceVisible": true
                    },
                    {
                      "id": "12",
                      "name": "Oreo",
                      "price": 190,
                      "image": "../../../assets/images/Cheesecakes/Oreo.png",
                      "dealSeller": true,
                      "outOfStock": false,
                      "category": "Cheesecakes",
                      "bestSeller": false,
                      "visible": true,
                      "description": "The cookie classic reimagined in cheesecake form chilled to perfection",
                      "imgAltText": "Square slice of Oreo cheesecake with cookies and cream topping.",
                      "discount": 0,
                      "containsEgg": false,
                      "isPriceVisible": true
                    },
                    {
                      "id": "13",
                      "name": "Biscoff",
                      "price": 230,
                      "image": "../../../assets/images/Cheesecakes/Biscoff.png",
                      "dealSeller": true,
                      "outOfStock": false,
                      "category": "Cheesecakes",
                      "bestSeller": false,
                      "visible": true,
                      "description": "Cool, creamy layers with the unmistakable crunch of Biscoff",
                      "imgAltText": "Square slice of Biscoff cheesecake with a Biscoff cookie crumble.",
                      "discount": 0,
                      "containsEgg": false,
                      "isPriceVisible": true
                    },
                    {
                      "id": "14",
                      "name": "Blueberry",
                      "price": 240,
                      "image": "../../../assets/images/Cheesecakes/Blueberry.png",
                      "dealSeller": true,
                      "outOfStock": false,
                      "category": "Cheesecakes",
                      "bestSeller": true,
                      "visible": true,
                      "description": "A generous crown of tangy blueberry on",
                      "imgAltText": "Square slice of classic cheesecake topped with a generous layer of tangy blueberry compote.",
                      "discount": 0,
                      "containsEgg": true,
                      "isPriceVisible": true
                    },
                    {
                      "id": "15",
                      "name": "Hazelnut",
                      "price": 160,
                      "image": "../../../assets/images/Tres Leches/Hazelnut.png",
                      "dealSeller": true,
                      "outOfStock": false,
                      "category": "Tres Leches",
                      "bestSeller": false,
                      "visible": true,
                      "description": "Soft cake soaked in hazelnut milk topped with whipped cream",
                      "imgAltText": "Long slice of Hazelnut Tres Leches cake soaked in milk, topped with whipped cream and hazelnuts.",
                      "discount": 0,
                      "containsEgg": false,
                      "isPriceVisible": true
                    }
                  ],
                  "categories": [
                    {
                      "id": "1",
                      "name": "Brownies"
                    },
                    {
                      "id": "2",
                      "name": "Cheesecakes"
                    },
                    {
                      "id": "3",
                      "name": "Loaf Cakes"
                    },
                    {
                      "id": "4",
                      "name": "Tres Leches"
                    }
                  ],
                  "users": [
                    {
                      "id": "1",
                      "username": "admin",
                      "password_pin": "admin123",
                      "role": "admin"
                    },
                    {
                      "id": "2",
                      "username": "customer1",
                      "password_pin": "pass123",
                      "role": "customer"
                    },
                    {
                      "id": "1762195520731",
                      "username": "customer2",
                      "password_pin": "customer2",
                      "role": "customer"
                    }
                  ],
                  "messages": [
                    {
                      "id": "084a",
                      "name": "Nitya Bhanu",
                      "email": "nityabhanu2001@gmail.com",
                      "message": "Nice website buddy!",
                      "timestamp": "2025-11-03T17:04:45.987Z",
                      "read": true
                    }
                  ],
                  "settings": {
                    "isFullFunctionalityEnabled": true
                  }
                }
                """;

        MapType mapType = objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
        Map<String, Object> data = objectMapper.readValue(jsonData, mapType);

        // Products
        CollectionType productListType = objectMapper.getTypeFactory().constructCollectionType(List.class, Product.class);
        List<Product> products = objectMapper.convertValue(data.get("products"), productListType);
        productRepository.saveAll(products);

        // Categories
        CollectionType categoryListType = objectMapper.getTypeFactory().constructCollectionType(List.class, Category.class);
        List<Category> categories = objectMapper.convertValue(data.get("categories"), categoryListType);
        categoryRepository.saveAll(categories);

        // Users
        CollectionType userListType = objectMapper.getTypeFactory().constructCollectionType(List.class, User.class);
        List<Map<String, Object>> userMaps = (List<Map<String, Object>>) data.get("users");
        List<User> users = userMaps.stream().map(userMap -> {
            User user = new User();
            user.setId((String) userMap.get("id"));
            user.setUsername((String) userMap.get("username"));
            user.setPassword(passwordEncoder.encode((String) userMap.get("password_pin"))); // Encode password
            user.setRole((String) userMap.get("role"));
            return user;
        }).toList();
        userRepository.saveAll(users);

        // Messages
        CollectionType messageListType = objectMapper.getTypeFactory().constructCollectionType(List.class, Message.class);
        List<Map<String, Object>> messageMaps = (List<Map<String, Object>>) data.get("messages");
        List<Message> messages = messageMaps.stream().map(messageMap -> {
            Message message = new Message();
            message.setId((String) messageMap.get("id"));
            message.setName((String) messageMap.get("name"));
            message.setEmail((String) messageMap.get("email"));
            message.setMessage((String) messageMap.get("message"));
            message.setTimestamp(Instant.parse((String) messageMap.get("timestamp")));
            message.setRead((Boolean) messageMap.get("read"));
            return message;
        }).toList();
        messageRepository.saveAll(messages);

        // Settings
        Map<String, Object> settingsMap = (Map<String, Object>) data.get("settings");
        Settings settings = new Settings();
        settings.setId("appSettings"); // Fixed ID as per model
        settings.setIsFullFunctionalityEnabled((Boolean) settingsMap.get("isFullFunctionalityEnabled"));
        settingsRepository.save(settings);
    }
}
