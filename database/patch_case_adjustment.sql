-- 案件延期/挂账 + 派遣员归属（执行库 cityguard）
USE cityguard;

-- 案件主表扩展
ALTER TABLE case_info
    ADD COLUMN dispatch_operator_id BIGINT NULL COMMENT '派遣操作员用户ID（派遣至部门时写入）' AFTER dispatch_time,
    ADD COLUMN dispatch_operator_name VARCHAR(50) NULL COMMENT '派遣操作员姓名' AFTER dispatch_operator_id,
    ADD COLUMN is_suspended TINYINT NOT NULL DEFAULT 0 COMMENT '是否挂账中：0否 1是' AFTER is_forced_close,
    ADD COLUMN suspend_until DATETIME NULL COMMENT '挂账恢复时间' AFTER is_suspended,
    ADD COLUMN extension_approved_count INT NOT NULL DEFAULT 0 COMMENT '已批准延期次数' AFTER suspend_until;

-- 历史案件：从流程记录回填派遣员
UPDATE case_info c
INNER JOIN (
    SELECT f.case_id, f.operator_id, f.operator_name
    FROM case_flow_record f
    INNER JOIN (
        SELECT case_id, MAX(id) AS max_id
        FROM case_flow_record
        WHERE node_name = '派遣至处置部门'
        GROUP BY case_id
    ) t ON f.id = t.max_id
) d ON c.id = d.case_id
SET c.dispatch_operator_id = d.operator_id,
    c.dispatch_operator_name = d.operator_name
WHERE c.dispatch_operator_id IS NULL AND d.operator_id IS NOT NULL;

-- 延期/挂账申请表
CREATE TABLE IF NOT EXISTS case_adjustment_apply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    case_id BIGINT NOT NULL COMMENT '案件ID',
    case_code VARCHAR(19) NOT NULL COMMENT '案件编码',
    apply_type VARCHAR(20) NOT NULL COMMENT 'extension=延期 suspend=挂账',
    apply_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected',
    reason VARCHAR(500) NOT NULL COMMENT '申请原因',
    suspend_until DATETIME NULL COMMENT '挂账恢复日期（挂账申请）',
    old_deadline_time DATETIME NULL COMMENT '申请时处置截止时间快照',
    new_deadline_time DATETIME NULL COMMENT '批准后新截止时间（延期）',
    applicant_id BIGINT NOT NULL COMMENT '申请人（处置部门账号）',
    applicant_name VARCHAR(50) NOT NULL COMMENT '申请人姓名',
    applicant_dept_id BIGINT NULL COMMENT '申请部门ID',
    reviewer_id BIGINT NULL COMMENT '审批派遣员ID',
    reviewer_name VARCHAR(50) NULL COMMENT '审批派遣员姓名',
    review_remark VARCHAR(500) NULL COMMENT '审批意见',
    review_time DATETIME NULL COMMENT '审批时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    INDEX idx_case_id (case_id),
    INDEX idx_apply_status (apply_status),
    INDEX idx_apply_type (apply_type),
    INDEX idx_suspend_until (suspend_until)
) COMMENT '案件延期/挂账申请';
