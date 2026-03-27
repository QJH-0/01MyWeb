CREATE TABLE IF NOT EXISTS rbac_roles (
    role_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    authority VARCHAR(50) NOT NULL,
    UNIQUE KEY uk_rbac_roles_authority (authority)
);

CREATE TABLE IF NOT EXISTS rbac_permissions (
    permission_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    authority VARCHAR(50) NOT NULL,
    UNIQUE KEY uk_rbac_permissions_authority (authority)
);

CREATE TABLE IF NOT EXISTS rbac_role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rbac_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES rbac_roles (role_id) ON DELETE CASCADE,
    CONSTRAINT fk_rbac_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES rbac_permissions (permission_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS rbac_user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_rbac_user_roles_user
        FOREIGN KEY (user_id) REFERENCES user_accounts (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_rbac_user_roles_role
        FOREIGN KEY (role_id) REFERENCES rbac_roles (role_id) ON DELETE CASCADE
);

-- Roles
INSERT INTO rbac_roles(authority)
SELECT 'ROLE_USER'
WHERE NOT EXISTS (SELECT 1 FROM rbac_roles WHERE authority = 'ROLE_USER');

INSERT INTO rbac_roles(authority)
SELECT 'ROLE_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM rbac_roles WHERE authority = 'ROLE_ADMIN');

-- Permissions
INSERT INTO rbac_permissions(authority)
SELECT 'PERM_AI_ACCESS'
WHERE NOT EXISTS (SELECT 1 FROM rbac_permissions WHERE authority = 'PERM_AI_ACCESS');

INSERT INTO rbac_permissions(authority)
SELECT 'PERM_COMMENT_WRITE'
WHERE NOT EXISTS (SELECT 1 FROM rbac_permissions WHERE authority = 'PERM_COMMENT_WRITE');

INSERT INTO rbac_permissions(authority)
SELECT 'PERM_ADMIN_PANEL'
WHERE NOT EXISTS (SELECT 1 FROM rbac_permissions WHERE authority = 'PERM_ADMIN_PANEL');

-- Role -> Permission mapping
INSERT INTO rbac_role_permissions(role_id, permission_id)
SELECT
    (SELECT role_id FROM rbac_roles WHERE authority = 'ROLE_USER'),
    (SELECT permission_id FROM rbac_permissions WHERE authority = 'PERM_AI_ACCESS')
WHERE NOT EXISTS (
    SELECT 1 FROM rbac_role_permissions rp
    WHERE rp.role_id = (SELECT role_id FROM rbac_roles WHERE authority = 'ROLE_USER')
      AND rp.permission_id = (SELECT permission_id FROM rbac_permissions WHERE authority = 'PERM_AI_ACCESS')
);

INSERT INTO rbac_role_permissions(role_id, permission_id)
SELECT
    (SELECT role_id FROM rbac_roles WHERE authority = 'ROLE_USER'),
    (SELECT permission_id FROM rbac_permissions WHERE authority = 'PERM_COMMENT_WRITE')
WHERE NOT EXISTS (
    SELECT 1 FROM rbac_role_permissions rp
    WHERE rp.role_id = (SELECT role_id FROM rbac_roles WHERE authority = 'ROLE_USER')
      AND rp.permission_id = (SELECT permission_id FROM rbac_permissions WHERE authority = 'PERM_COMMENT_WRITE')
);

INSERT INTO rbac_role_permissions(role_id, permission_id)
SELECT
    (SELECT role_id FROM rbac_roles WHERE authority = 'ROLE_ADMIN'),
    (SELECT permission_id FROM rbac_permissions WHERE authority = 'PERM_ADMIN_PANEL')
WHERE NOT EXISTS (
    SELECT 1 FROM rbac_role_permissions rp
    WHERE rp.role_id = (SELECT role_id FROM rbac_roles WHERE authority = 'ROLE_ADMIN')
      AND rp.permission_id = (SELECT permission_id FROM rbac_permissions WHERE authority = 'PERM_ADMIN_PANEL')
);

INSERT INTO rbac_role_permissions(role_id, permission_id)
SELECT
    (SELECT role_id FROM rbac_roles WHERE authority = 'ROLE_ADMIN'),
    (SELECT permission_id FROM rbac_permissions WHERE authority = 'PERM_AI_ACCESS')
WHERE NOT EXISTS (
    SELECT 1 FROM rbac_role_permissions rp
    WHERE rp.role_id = (SELECT role_id FROM rbac_roles WHERE authority = 'ROLE_ADMIN')
      AND rp.permission_id = (SELECT permission_id FROM rbac_permissions WHERE authority = 'PERM_AI_ACCESS')
);

INSERT INTO rbac_role_permissions(role_id, permission_id)
SELECT
    (SELECT role_id FROM rbac_roles WHERE authority = 'ROLE_ADMIN'),
    (SELECT permission_id FROM rbac_permissions WHERE authority = 'PERM_COMMENT_WRITE')
WHERE NOT EXISTS (
    SELECT 1 FROM rbac_role_permissions rp
    WHERE rp.role_id = (SELECT role_id FROM rbac_roles WHERE authority = 'ROLE_ADMIN')
      AND rp.permission_id = (SELECT permission_id FROM rbac_permissions WHERE authority = 'PERM_COMMENT_WRITE')
);

