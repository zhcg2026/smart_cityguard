-- 修改 sys_user 表的唯一约束
-- 将 username 单独唯一改为 (username, deleted) 组合唯一
-- 这样允许已删除的用户名可以重新使用

-- 1. 删除旧的唯一约束
ALTER TABLE sys_user DROP INDEX uk_username;

-- 2. 添加新的组合唯一约束
ALTER TABLE sys_user ADD UNIQUE INDEX uk_username_deleted (username, deleted);

-- 验证修改
SHOW INDEX FROM sys_user WHERE Key_name = 'uk_username_deleted';