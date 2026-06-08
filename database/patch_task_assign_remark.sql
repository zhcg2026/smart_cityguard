-- 核查/核实任务：保存受理员下发时的要求备注
ALTER TABLE check_task
    ADD COLUMN assign_remark VARCHAR(500) NULL COMMENT '下发要求/备注' AFTER assigner_name;

ALTER TABLE verify_task
    ADD COLUMN assign_remark VARCHAR(500) NULL COMMENT '下发要求/备注' AFTER creator_name;
