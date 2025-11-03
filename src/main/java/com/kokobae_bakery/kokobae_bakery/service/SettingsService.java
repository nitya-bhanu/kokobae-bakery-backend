package com.kokobae_bakery.kokobae_bakery.service;

import com.kokobae_bakery.kokobae_bakery.model.Settings;
import com.kokobae_bakery.kokobae_bakery.repository.SettingsRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SettingsService {

    private final SettingsRepository settingsRepository;

    public SettingsService(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public Settings getSettings() {
        // Assuming a single settings document with a fixed ID "appSettings"
        return settingsRepository.findById("appSettings").orElseGet(() -> {
            Settings defaultSettings = new Settings();
            defaultSettings.setIsFullFunctionalityEnabled(true); // Default value
            return settingsRepository.save(defaultSettings);
        });
    }

    public Settings updateFullFunctionalityFlag(boolean value) {
        Settings settings = getSettings(); // Get existing or create default
        settings.setIsFullFunctionalityEnabled(value);
        return settingsRepository.save(settings);
    }
}
