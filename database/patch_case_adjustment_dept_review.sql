-- 延期/挂账：处置人员申请 → 处置部门初审 → 派遣员终审
USE cityguard;

ALTER TABLE case_adjustment_apply
    ADD COLUMN dept_reviewer_id BIGINT NULL COMMENT '部门初审人ID' AFTER applicant_dept_id,
    ADD COLUMN dept_reviewer_name VARCHAR(50) NULL COMMENT '部门初审人姓名' AFTER dept_reviewer_id,
    ADD COLUMN dept_review_remark VARCHAR(500) NULL COMMENT '部门初审意见' AFTER dept_reviewer_name,
    ADD COLUMN dept_review_time DATETIME NULL COMMENT '部门初审时间' AFTER dept_review_remark;

-- apply_status 新增取值 pending_dept（待部门审核），原 pending 表示待派遣员审批
