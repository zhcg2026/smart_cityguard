-- 处置超时申诉：案件豁免字段（库 cityguard）
USE cityguard;

ALTER TABLE case_info
    ADD COLUMN handle_timeout_exempt TINYINT NOT NULL DEFAULT 0 COMMENT '处置超时申诉通过：0否 1统计按未超时' AFTER appeal_status,
    ADD COLUMN handle_timeout_exempt_appeal_id BIGINT NULL COMMENT '豁免关联 appeal_apply.id' AFTER handle_timeout_exempt;
