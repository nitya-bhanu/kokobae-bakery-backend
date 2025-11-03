package com.kokobae_bakery.kokobae_bakery.controller;

import com.kokobae_bakery.kokobae_bakery.model.Settings;
import com.kokobae_bakery.kokobae_bakery.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<Settings> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PatchMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Settings> updateFullFunctionalityFlag(@RequestBody Boolean isFullFunctionalityEnabled) {
        Settings updatedSettings = settingsService.updateFullFunctionalityFlag(isFullFunctionalityEnabled);
        return ResponseEntity.ok(updatedSettings);
    }
}
