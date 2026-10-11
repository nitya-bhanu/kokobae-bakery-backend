package com.kokobae_bakery.kokobae_bakery.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tags")
public class Tag {
    @Id
    private String id;
    private String name;
    private String colorHex;
    private String textColorHex;
}
