CREATE TABLE IF NOT EXISTS content_page (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    page_key VARCHAR(50) NOT NULL,
    title VARCHAR(200) NULL,
    summary TEXT NULL,
    sections_json TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_content_page_page_key UNIQUE (page_key)
);

INSERT INTO content_page (page_key, title, summary, sections_json, created_at, updated_at)
SELECT 'home', '首页', '门户首页内容（M1 占位）。', '[]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM content_page WHERE page_key = 'home');

INSERT INTO content_page (page_key, title, summary, sections_json, created_at, updated_at)
SELECT 'about', '关于', '关于页内容（M1 占位）。', '[]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM content_page WHERE page_key = 'about');

INSERT INTO content_page (page_key, title, summary, sections_json, created_at, updated_at)
SELECT 'experience', '经历', '经历页内容（M1 占位）。', '[]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM content_page WHERE page_key = 'experience');

