package com.kokobae_bakery.kokobae_bakery.service;

import com.kokobae_bakery.kokobae_bakery.model.Tag;
import com.kokobae_bakery.kokobae_bakery.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    public Optional<Tag> getTagById(String id) {
        return tagRepository.findById(id);
    }

    public Tag createTag(Tag tag) {
        tag.setId(null);
        return tagRepository.save(tag);
    }

    public Tag updateTag(String id, Tag tagDetails) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tag not found with id " + id));
        tag.setName(tagDetails.getName());
        tag.setColorHex(tagDetails.getColorHex());
        tag.setTextColorHex(tagDetails.getTextColorHex());
        return tagRepository.save(tag);
    }

    public void deleteTag(String id) {
        tagRepository.deleteById(id);
    }
}
