-- Normalize RBAC: roles are derived from rbac_user_roles, not from user_accounts.roles.
ALTER TABLE user_accounts
    DROP COLUMN roles;

