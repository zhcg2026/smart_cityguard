-- 案件计时：小类时限覆盖表（在已有库上执行一次）
CREATE TABLE IF NOT EXISTS category_time_limit_override (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    small_id BIGINT NOT NULL COMMENT '小类ID',
    time_limit_type VARCHAR(20) NOT NULL COMMENT '时限类型',
    time_limit_value INT NOT NULL COMMENT '时限数值',
    remark VARCHAR(200) COMMENT '备注',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_small_id (small_id)
) COMMENT '小类处置时限覆盖配置';

-- case_timer_record 计时暂停字段（挂账恢复用；列已存在时跳过本段）
-- ALTER TABLE case_timer_record
--     ADD COLUMN pause_start_time DATETIME NULL COMMENT '挂账暂停开始时间' AFTER timer_status,
--     ADD COLUMN total_paused_seconds INT DEFAULT 0 COMMENT '累计暂停秒数' AFTER pause_start_time;
