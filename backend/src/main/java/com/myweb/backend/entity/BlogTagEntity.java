package com.myweb.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 博客标签行：与 {@link BlogEntity} 一对多，管理端全量替换写入。 */
@Entity
@Table(name = "blog_tag")
public class BlogTagEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blog_id", nullable = false)
    private Long blogId;

    @Column(name = "tag", nullable = false, length = 60)
    private String tag;

    public BlogTagEntity() {
    }

    public BlogTagEntity(Long blogId, String tag) {
        this.blogId = blogId;
        this.tag = tag;
    }

    public Long getId() {
        return id;
    }

    public Long getBlogId() {
        return blogId;
    }

    public void setBlogId(Long blogId) {
        this.blogId = blogId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
