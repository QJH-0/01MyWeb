CREATE TABLE IF NOT EXISTS blog (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(200) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    content LONGTEXT NULL,
    category VARCHAR(80) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    cover_url VARCHAR(1000) NULL,
    view_count INT NOT NULL DEFAULT 0,
    published_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL
);

CREATE UNIQUE INDEX uk_blog_slug ON blog (slug);

CREATE INDEX idx_blog_status ON blog (status);
CREATE INDEX idx_blog_category ON blog (category);
CREATE INDEX idx_blog_published_at ON blog (published_at);

CREATE TABLE IF NOT EXISTS blog_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    blog_id BIGINT NOT NULL,
    tag VARCHAR(60) NOT NULL,
    CONSTRAINT fk_blog_tag_blog_id FOREIGN KEY (blog_id) REFERENCES blog (id)
);

CREATE INDEX idx_blog_tag_blog_id ON blog_tag (blog_id);
CREATE INDEX idx_blog_tag_tag ON blog_tag (tag);
