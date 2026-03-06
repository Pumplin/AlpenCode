-- OJ 题库管理菜单注册
-- 执行前请确认 sys_menu 表中 3000-3099 ID 段未被占用

-- 1. 一级目录：OJ 题库管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (3000, 'OJ 题库管理', 0, 5, 'oj', NULL, 1, 0, 'M', '0', '0', '', 'code', 'admin', NOW(), '', NULL, 'OJ 在线判题系统');

-- 2. 二级菜单：题目管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (3001, '题目管理', 3000, 1, 'problem', 'oj/problem/index', 1, 0, 'C', '0', '0', 'oj:problem:list', 'file-text', 'admin', NOW(), '', NULL, '题目列表页');

-- 3. 二级菜单：分类管理
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (3002, '分类管理', 3000, 2, 'category', 'oj/category/index', 1, 0, 'C', '0', '0', 'oj:category:list', 'tags', 'admin', NOW(), '', NULL, '题目分类管理');

-- 4. 隐藏菜单：题目详情（使用 query 参数，parent_id=3000 与题目管理同级，path 不带 :id）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (3003, '题目详情', 3000, 3, 'problem/detail', 'oj/problem/detail', 1, 0, 'C', '1', '0', 'oj:problem:query', '#', 'admin', NOW(), '', NULL, '题目详情页（隐藏菜单，query传参）');

-- 5. 题目管理按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES 
(3010, '题目查询', 3001, 1, '', '', 1, 0, 'F', '0', '0', 'oj:problem:query', '#', 'admin', NOW(), '', NULL, ''),
(3011, '题目新增', 3001, 2, '', '', 1, 0, 'F', '0', '0', 'oj:problem:add', '#', 'admin', NOW(), '', NULL, ''),
(3012, '题目修改', 3001, 3, '', '', 1, 0, 'F', '0', '0', 'oj:problem:edit', '#', 'admin', NOW(), '', NULL, ''),
(3013, '题目删除', 3001, 4, '', '', 1, 0, 'F', '0', '0', 'oj:problem:remove', '#', 'admin', NOW(), '', NULL, '');

-- 6. 分类管理按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES 
(3020, '分类查询', 3002, 1, '', '', 1, 0, 'F', '0', '0', 'oj:category:query', '#', 'admin', NOW(), '', NULL, ''),
(3021, '分类新增', 3002, 2, '', '', 1, 0, 'F', '0', '0', 'oj:category:add', '#', 'admin', NOW(), '', NULL, ''),
(3022, '分类修改', 3002, 3, '', '', 1, 0, 'F', '0', '0', 'oj:category:edit', '#', 'admin', NOW(), '', NULL, ''),
(3023, '分类删除', 3002, 4, '', '', 1, 0, 'F', '0', '0', 'oj:category:remove', '#', 'admin', NOW(), '', NULL, '');

-- 7. 测试用例管理按钮权限（挂在题目管理下）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES 
(3014, '用例查询', 3001, 5, '', '', 1, 0, 'F', '0', '0', 'oj:testCase:query', '#', 'admin', NOW(), '', NULL, ''),
(3015, '用例新增', 3001, 6, '', '', 1, 0, 'F', '0', '0', 'oj:testCase:add', '#', 'admin', NOW(), '', NULL, ''),
(3016, '用例修改', 3001, 7, '', '', 1, 0, 'F', '0', '0', 'oj:testCase:edit', '#', 'admin', NOW(), '', NULL, ''),
(3017, '用例删除', 3001, 8, '', '', 1, 0, 'F', '0', '0', 'oj:testCase:remove', '#', 'admin', NOW(), '', NULL, '');

-- 8. 给超级管理员角色授权（role_id=1）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 3000 AND 3099;

-- 验证 SQL
-- SELECT * FROM sys_menu WHERE menu_id BETWEEN 3000 AND 3099 ORDER BY menu_id;
-- SELECT * FROM sys_role_menu WHERE menu_id BETWEEN 3000 AND 3099;
