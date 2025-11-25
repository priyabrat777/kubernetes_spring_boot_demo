package com.demo.k8s.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * Data item entity for demonstration purposes.
 * Implements Serializable for Redis caching support.
 */
@Entity
@Table(name = "data_items")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @NotBlank
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "timestamp", nullable = false)
    private long timestamp;

    public DataItem() {
        this.timestamp = System.currentTimeMillis();
    }

    public DataItem(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
