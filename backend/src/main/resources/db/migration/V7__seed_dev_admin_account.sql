-- 本地/联调默认管理员（Flyway 一次性种子）。
-- 用户名：myweb-admin；明文密码仅用于开发：MyWebAdm1n2026（BCrypt 由 Spring 默认强度生成）。
-- 生产环境：首次迁移后请立即改密，或删除该用户改用 APP_ADMIN_BOOTSTRAP_* + 强密码策略。

INSERT INTO user_accounts (username, password_hash)
SELECT 'myweb-admin', '$2a$10$69afvJ03EwtiIArexHhh6OiwdgEsn6Pgnd06esghsD12tXAZIzGlS'
WHERE NOT EXISTS (SELECT 1 FROM user_accounts WHERE username = 'myweb-admin');

INSERT INTO rbac_user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM user_accounts u
JOIN rbac_roles r ON r.authority = 'ROLE_ADMIN'
WHERE u.username = 'myweb-admin'
  AND NOT EXISTS (
    SELECT 1 FROM rbac_user_roles ur
    WHERE ur.user_id = u.user_id AND ur.role_id = r.role_id
);

INSERT INTO rbac_user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM user_accounts u
JOIN rbac_roles r ON r.authority = 'ROLE_USER'
WHERE u.username = 'myweb-admin'
  AND NOT EXISTS (
    SELECT 1 FROM rbac_user_roles ur
    WHERE ur.user_id = u.user_id AND ur.role_id = r.role_id
);
