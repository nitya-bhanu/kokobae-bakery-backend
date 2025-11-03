package com.kokobae_bakery.kokobae_bakery.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "settings")
public class Settings {
    @Id
    private String id = "appSettings"; // Fixed ID for a singleton document
    private Boolean isFullFunctionalityEnabled;
}
