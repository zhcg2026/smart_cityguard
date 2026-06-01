-- 修正立案条件：文案含「紧急」但 handle_time_type 仍为 work_hour/work_day 的旧数据
-- （muban 解析规则修正前导入的标准）。执行后须重启后端；已派遣案件的计时记录需新案或重新派遣才刷新。
-- 建议：cmd /c "chcp 65001 >nul && mysql ... --default-character-set=utf8mb4 < patch_fix_urgent_handle_time_type.sql"

USE cityguard;

UPDATE case_standard
SET handle_time_type = 'urgent_hour',
    handle_time_limit = CONCAT(handle_time_value, '紧急工作时')
WHERE is_deleted = 0
  AND handle_time_limit LIKE '%紧急工作时%'
  AND handle_time_type <> 'urgent_hour';

UPDATE case_standard
SET handle_time_type = 'natural_day',
    handle_time_limit = CONCAT(handle_time_value, '紧急工作日')
WHERE is_deleted = 0
  AND handle_time_limit LIKE '%紧急工作日%'
  AND handle_time_type NOT IN ('natural_day');
