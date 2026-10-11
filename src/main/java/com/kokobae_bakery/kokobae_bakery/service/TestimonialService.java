package com.kokobae_bakery.kokobae_bakery.service;

import com.kokobae_bakery.kokobae_bakery.model.Testimonial;
import com.kokobae_bakery.kokobae_bakery.repository.TestimonialRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class TestimonialService {

    private final TestimonialRepository testimonialRepository;

    public TestimonialService(TestimonialRepository testimonialRepository) {
        this.testimonialRepository = testimonialRepository;
    }

    public List<Testimonial> getVisibleTestimonials() {
        return testimonialRepository.findByVisibleTrueOrderByDisplayOrderAsc();
    }

    public List<Testimonial> getAllTestimonials() {
        return testimonialRepository.findAll();
    }

    public Optional<Testimonial> getTestimonialById(String id) {
        return testimonialRepository.findById(id);
    }

    public Testimonial createTestimonial(Testimonial testimonial) {
        testimonial.setId(null);
        applyDefaults(testimonial);
        return testimonialRepository.save(testimonial);
    }

    public Testimonial updateTestimonial(String id, Testimonial details) {
        Testimonial testimonial = testimonialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Testimonial not found with id " + id));
        testimonial.setCustomerName(details.getCustomerName());
        testimonial.setCustomerRole(details.getCustomerRole());
        testimonial.setRating(details.getRating());
        testimonial.setComment(details.getComment());
        testimonial.setAvatarUrl(details.getAvatarUrl());
        testimonial.setProductMentioned(details.getProductMentioned());
        testimonial.setVisible(details.getVisible());
        testimonial.setDisplayOrder(details.getDisplayOrder());
        applyDefaults(testimonial);
        return testimonialRepository.save(testimonial);
    }

    public Testimonial setVisibility(String id, boolean visible) {
        Testimonial testimonial = testimonialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Testimonial not found with id " + id));
        testimonial.setVisible(visible);
        return testimonialRepository.save(testimonial);
    }

    public void deleteTestimonial(String id) {
        testimonialRepository.deleteById(id);
    }

    private void applyDefaults(Testimonial testimonial) {
        if (testimonial.getVisible() == null) {
            testimonial.setVisible(true);
        }
        if (testimonial.getCreatedAt() == null) {
            testimonial.setCreatedAt(Instant.now());
        }
    }
}
