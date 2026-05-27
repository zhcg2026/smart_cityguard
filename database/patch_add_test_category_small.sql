-- 为已有库增加测试用小类（挂在「事件-市容环境」大类下）
-- 执行：mysql -u root -p cityguard < database/patch_add_test_category_small.sql
-- 可重复执行：依赖 uk_small_code_type 做幂等更新

INSERT INTO category_small (small_code, small_name, big_id, big_code, category_type, full_code, description, sort_order, status, is_deleted)
SELECT '99', '测试小类（联调）', id, big_code, category_type, CONCAT(big_code, '99'), '用于采集端上报等功能测试', 999, 1, 0
FROM category_big
WHERE category_type = 'event' AND big_code = '01' AND is_deleted = 0
LIMIT 1
ON DUPLICATE KEY UPDATE
  small_name = VALUES(small_name),
  description = VALUES(description),
  sort_order = VALUES(sort_order),
  status = 1,
  is_deleted = 0;
