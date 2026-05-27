-- ============================================
-- 片区与采集员：由单人 user_id 改为多对多关联表
-- ============================================

USE cityguard;

CREATE TABLE IF NOT EXISTS responsibility_grid_collector (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    resp_grid_id BIGINT NOT NULL COMMENT '责任片区ID',
    user_id BIGINT NOT NULL COMMENT '采集员用户ID(sys_user.id)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_grid_user (resp_grid_id, user_id),
    KEY idx_rgc_user (user_id),
    CONSTRAINT fk_rgc_resp_grid FOREIGN KEY (resp_grid_id) REFERENCES responsibility_grid (id) ON DELETE CASCADE
) COMMENT '片区-采集员多对多';

-- 将原 responsibility_grid.user_id 迁入关联表（可重复执行：冲突则忽略）
INSERT IGNORE INTO responsibility_grid_collector (resp_grid_id, user_id)
SELECT id, user_id
FROM responsibility_grid
WHERE user_id IS NOT NULL AND is_deleted = 0;

-- 清空旧单人字段（WHERE 含主键 id，兼容 MySQL Workbench 安全更新模式）
UPDATE responsibility_grid SET user_id = NULL WHERE id > 0 AND user_id IS NOT NULL;
