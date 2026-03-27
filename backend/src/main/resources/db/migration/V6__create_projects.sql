CREATE TABLE IF NOT EXISTS project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    description TEXT NULL,
    category VARCHAR(80) NULL,
    cover_url VARCHAR(1000) NULL,
    project_url VARCHAR(1000) NULL,
    source_url VARCHAR(1000) NULL,
    visible TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL
);

CREATE INDEX idx_project_category ON project (category);
CREATE INDEX idx_project_visible ON project (visible);
CREATE INDEX idx_project_sort_order ON project (sort_order);

CREATE TABLE IF NOT EXISTS project_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    tag VARCHAR(60) NOT NULL,
    CONSTRAINT fk_project_tag_project_id FOREIGN KEY (project_id) REFERENCES project (id)
);

CREATE INDEX idx_project_tag_project_id ON project_tag (project_id);
CREATE INDEX idx_project_tag_tag ON project_tag (tag);

