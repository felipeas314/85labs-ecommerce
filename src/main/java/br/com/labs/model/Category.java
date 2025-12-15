package br.com.labs.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Category {

    private UUID id;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    public Category() {}

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Category category = new Category();

        public Builder id(UUID id) {
            category.id = id;
            return this;
        }

        public Builder name(String name) {
            category.name = name;
            return this;
        }

        public Builder description(String description) {
            category.description = description;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            category.createdAt = createdAt;
            return this;
        }

        public Category build() {
            return category;
        }
    }
}
