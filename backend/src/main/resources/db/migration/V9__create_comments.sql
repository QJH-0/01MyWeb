-- 评论与点赞：表名使用 comments / comment_likes，避免 H2 等对保留字 comment 的解析问题。
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    author_user_id BIGINT NOT NULL,
    content LONGTEXT NOT NULL,
    like_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL
);

CREATE INDEX idx_comments_target ON comments (target_type, target_id);
CREATE INDEX idx_comments_parent ON comments (parent_id);
CREATE INDEX idx_comments_created ON comments (created_at);

CREATE TABLE IF NOT EXISTS comment_likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_comment_likes_pair UNIQUE (comment_id, user_id),
    CONSTRAINT fk_comment_likes_comment FOREIGN KEY (comment_id) REFERENCES comments (id)
);

CREATE INDEX idx_comment_likes_comment_id ON comment_likes (comment_id);
