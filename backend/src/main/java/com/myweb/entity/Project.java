package com.myweb.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(
    name = "project",
    uniqueConstraints = @UniqueConstraint(name = "uk_project_title", columnNames = {"title"})
)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(length = 80)
    private String category;

    @ElementCollection
    @CollectionTable(name = "project_tag", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "tag", length = 60)
    private List<String> tags = new ArrayList<>();

    @Column(length = 1000)
    private String coverUrl;

    @Column(length = 1000)
    private String githubUrl;

    @Column(length = 1000)
    private String demoUrl;

    @Column(nullable = false)
    private boolean visible = true;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getCategory() {
        return category;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public String getDemoUrl() {
        return demoUrl;
    }

    public boolean isVisible() {
        return visible;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public void setDemoUrl(String demoUrl) {
        this.demoUrl = demoUrl;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}

