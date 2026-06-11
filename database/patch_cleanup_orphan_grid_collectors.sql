-- 清理片区-采集员关联表中指向已删除/不存在用户的脏数据（可重复执行）
USE cityguard;

DELETE rgc FROM responsibility_grid_collector rgc
LEFT JOIN sys_user u ON u.id = rgc.user_id AND u.deleted = 0
WHERE u.id IS NULL;
