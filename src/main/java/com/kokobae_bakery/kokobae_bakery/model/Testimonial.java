package com.kokobae_bakery.kokobae_bakery.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "testimonials")
public class Testimonial {
    @Id
    private String id;
    private String customerName;
    private String customerRole;
    private Integer rating;
    private String comment;
    private String avatarUrl;
    private String productMentioned;
    private Boolean visible;
    private Integer displayOrder;
    private Instant createdAt;
}
