package com.kokobae_bakery.kokobae_bakery.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    private String name;
    private Double price;
    private String image;
    private Boolean dealSeller;
    private Boolean outOfStock;
    private String category;
    private Boolean bestSeller;
    private Boolean visible;
    private String description;
    private String imgAltText;
    private Integer discount;
    private Boolean containsEgg;
    private Boolean isPriceVisible;
}
