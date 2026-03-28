package com.myweb.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 项目标签行：按 project_id 批量替换写入，避免逗号分隔字符串难以索引与去重。 */
@Entity
@Table(name = "project_tag")
public class ProjectTagEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "tag", nullable = false, length = 60)
    private String tag;

    public ProjectTagEntity() {
    }

    public ProjectTagEntity(Long projectId, String tag) {
        this.projectId = projectId;
        this.tag = tag;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}

