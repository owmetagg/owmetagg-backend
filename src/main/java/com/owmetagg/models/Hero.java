package com.owmetagg.models;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "heroes")
@Data
public class Hero {

    @Id
    @JsonProperty("key")
    @Column(name = "hero_key", nullable = false)
    private String heroKey;

    @JsonProperty("name")
    @Column(name = "name", nullable = false)
    private String name;

    @JsonProperty("role")
    @Column(name = "role", nullable = false)
    private String role;

    @JsonProperty("portrait")
    @Column(name = "portrait_url")
    private String portraitUrl;

    @JsonProperty("description")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JsonProperty("health")
    @Column(name = "health")
    private Integer health;

    @JsonProperty("armor")
    @Column(name = "armor")
    private Integer armor;

    @JsonProperty("shield")
    @Column(name = "shield")
    private Integer shield;

    @JsonProperty("real_name")
    @Column(name = "real_name")
    private String realName;

    @JsonProperty("age")
    @Column(name = "age")
    private Integer age;

    @JsonProperty("nationality")
    @Column(name = "nationality")
    private String nationality;

    @JsonProperty("occupation")
    @Column(name = "occupation")
    private String occupation;

    @JsonProperty("base_of_operations")
    @Column(name = "base_of_operations")
    private String baseOfOperations;

    @JsonProperty("affiliation")
    @Column(name = "affiliation")
    private String affiliation;

    @JsonProperty("difficulty")
    @Column(name = "difficulty")
    private Integer difficulty; // 1-3 stars

    @JsonProperty("release_date")
    @Column(name = "release_date")
    private String releaseDate;

    // Computed total health (health + armor + shield)
    public Integer getTotalHealth() {
        int total = (health != null ? health : 0);
        total += (armor != null ? armor : 0);
        total += (shield != null ? shield : 0);
        return total > 0 ? total : null;
    }

    // Helper method to get display name
    public String getDisplayName() {
        return name != null ? name : heroKey;
    }

    // Helper method to check if hero is a specific role
    public boolean isTank() {
        return "tank".equalsIgnoreCase(role);
    }

    public boolean isDamage() {
        return "damage".equalsIgnoreCase(role);
    }

    public boolean isSupport() {
        return "support".equalsIgnoreCase(role);
    }
}