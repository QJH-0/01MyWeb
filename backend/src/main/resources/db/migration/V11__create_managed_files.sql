CREATE TABLE IF NOT EXISTS managed_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    access_url VARCHAR(500) NULL,
    uploaded_by VARCHAR(50) NULL,
    created_at DATETIME NOT NULL,
    deleted_at DATETIME NULL
);

CREATE UNIQUE INDEX uk_managed_file_storage_key ON managed_file (storage_key);

CREATE INDEX idx_managed_file_file_type ON managed_file (file_type);

CREATE INDEX idx_managed_file_created_at ON managed_file (created_at);
