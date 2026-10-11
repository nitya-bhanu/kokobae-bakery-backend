package com.kokobae_bakery.kokobae_bakery.controller;

import com.kokobae_bakery.kokobae_bakery.model.Testimonial;
import com.kokobae_bakery.kokobae_bakery.service.TestimonialService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/testimonials")
public class TestimonialController {

    private final TestimonialService testimonialService;

    public TestimonialController(TestimonialService testimonialService) {
        this.testimonialService = testimonialService;
    }

    @GetMapping
    public List<Testimonial> getVisibleTestimonials() {
        return testimonialService.getVisibleTestimonials();
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Testimonial> getAllTestimonials() {
        return testimonialService.getAllTestimonials();
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Testimonial createTestimonial(@RequestBody Testimonial testimonial) {
        return testimonialService.createTestimonial(testimonial);
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Testimonial> updateTestimonial(@PathVariable String id, @RequestBody Testimonial details) {
        try {
            return ResponseEntity.ok(testimonialService.updateTestimonial(id, details));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/admin/{id}/visibility")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Testimonial> setVisibility(@PathVariable String id, @RequestParam boolean visible) {
        try {
            return ResponseEntity.ok(testimonialService.setVisibility(id, visible));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTestimonial(@PathVariable String id) {
        testimonialService.deleteTestimonial(id);
        return ResponseEntity.noContent().build();
    }
}
