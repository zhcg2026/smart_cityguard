-- 若 user_message 表仍是旧结构（title/is_read），可执行本脚本对齐 init.sql 设计
-- 执行前请备份。若列已存在会报错，可忽略对应语句。

ALTER TABLE user_message
    ADD COLUMN msg_record_id BIGINT NULL COMMENT '消息发送记录ID' AFTER user_id,
    ADD COLUMN msg_code VARCHAR(20) NULL COMMENT '消息编码' AFTER msg_record_id,
    ADD COLUMN msg_type VARCHAR(20) NULL COMMENT '消息类型' AFTER msg_code,
    ADD COLUMN msg_title VARCHAR(100) NULL COMMENT '消息标题' AFTER msg_type,
    ADD COLUMN msg_content VARCHAR(500) NULL COMMENT '消息内容' AFTER msg_title,
    ADD COLUMN biz_type VARCHAR(20) NULL COMMENT '业务类型' AFTER msg_content,
    ADD COLUMN biz_id BIGINT NULL COMMENT '业务ID' AFTER biz_type,
    ADD COLUMN biz_code VARCHAR(30) NULL COMMENT '业务编码' AFTER biz_id,
    ADD COLUMN msg_status VARCHAR(20) DEFAULT 'unread' COMMENT '消息状态' AFTER biz_code,
    ADD COLUMN read_time DATETIME NULL COMMENT '阅读时间' AFTER msg_status,
    ADD COLUMN msg_time DATETIME NULL COMMENT '消息产生时间' AFTER read_time;

UPDATE user_message SET msg_title = title WHERE msg_title IS NULL AND title IS NOT NULL;
UPDATE user_message SET msg_content = content WHERE msg_content IS NULL AND content IS NOT NULL;
UPDATE user_message SET msg_status = IF(is_read = 1, 'read', 'unread') WHERE msg_status IS NULL AND is_read IS NOT NULL;
UPDATE user_message SET msg_time = create_time WHERE msg_time IS NULL;
UPDATE user_message SET msg_record_id = IFNULL(biz_id, 0) WHERE msg_record_id IS NULL;
UPDATE user_message SET msg_type = IFNULL(biz_type, 'system') WHERE msg_type IS NULL;
