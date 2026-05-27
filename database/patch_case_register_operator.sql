-- 立案受理员（闭环：受理员立案 → 受理员结案）
USE cityguard;

ALTER TABLE case_info
    ADD COLUMN register_operator_id BIGINT NULL COMMENT '立案受理员用户ID' AFTER accept_time,
    ADD COLUMN register_operator_name VARCHAR(50) NULL COMMENT '立案受理员姓名' AFTER register_operator_id;

-- 历史案件：从流程记录回填立案操作人（可选）
UPDATE case_info c
    INNER JOIN (
        SELECT case_id, operator_id, operator_name
        FROM case_flow_record
        WHERE node_name = '立案并批转'
    ) f ON c.id = f.case_id
SET c.register_operator_id = f.operator_id,
    c.register_operator_name = f.operator_name
WHERE c.register_operator_id IS NULL AND f.operator_id IS NOT NULL;
