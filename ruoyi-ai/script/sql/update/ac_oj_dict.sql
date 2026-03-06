-- =============================================
-- OJ 业务字典数据
-- ac_sample_type: 是否公开样例
-- ac_judge_result: 判题结果
-- ac_language: 编程语言
-- =============================================

-- 字典类型
INSERT INTO `sys_dict_type` VALUES (100, '000000', '是否公开样例', 'ac_sample_type', '0', NULL, NULL, NOW(), NULL, NOW(), '测试用例是否公开');
INSERT INTO `sys_dict_type` VALUES (101, '000000', '判题结果', 'ac_judge_result', '0', NULL, NULL, NOW(), NULL, NOW(), 'OJ判题结果状态');
INSERT INTO `sys_dict_type` VALUES (102, '000000', '编程语言', 'ac_language', '0', NULL, NULL, NOW(), NULL, NOW(), 'OJ支持的编程语言');

-- ac_sample_type 字典数据
INSERT INTO `sys_dict_data` VALUES (100, '000000', 0, '隐藏', '0', 'ac_sample_type', '', 'default', 'N', '0', NULL, NULL, NOW(), NULL, NOW(), NULL);
INSERT INTO `sys_dict_data` VALUES (101, '000000', 1, '公开', '1', 'ac_sample_type', 'success', 'success', 'N', '0', NULL, NULL, NOW(), NULL, NOW(), NULL);

-- ac_judge_result 字典数据
INSERT INTO `sys_dict_data` VALUES (110, '000000', 0, '待判题', '0', 'ac_judge_result', '', 'default', 'N', '0', NULL, NULL, NOW(), NULL, NOW(), NULL);
INSERT INTO `sys_dict_data` VALUES (111, '000000', 1, '判题中', '1', 'ac_judge_result', '', 'processing', 'N', '0', NULL, NULL, NOW(), NULL, NOW(), NULL);
INSERT INTO `sys_dict_data` VALUES (112, '000000', 2, 'AC', '2', 'ac_judge_result', 'success', 'success', 'N', '0', NULL, NULL, NOW(), NULL, NOW(), NULL);
INSERT INTO `sys_dict_data` VALUES (113, '000000', 3, 'WA', '3', 'ac_judge_result', '', 'danger', 'N', '0', NULL, NULL, NOW(), NULL, NOW(), NULL);
INSERT INTO `sys_dict_data` VALUES (114, '000000', 4, 'TLE', '4', 'ac_judge_result', '', 'warning', 'N', '0', NULL, NULL, NOW(), NULL, NOW(), NULL);
INSERT INTO `sys_dict_data` VALUES (115, '000000', 5, 'MLE', '5', 'ac_judge_result', '', 'warning', 'N', '0', NULL, NULL, NOW(), NULL, NOW(), NULL);
INSERT INTO `sys_dict_data` VALUES (116, '000000', 6, 'RE', '6', 'ac_judge_result', '', 'danger', 'N', '0', NULL, NULL, NOW(), NULL, NOW(), NULL);
INSERT INTO `sys_dict_data` VALUES (117, '000000', 7, 'CE', '7', 'ac_judge_result', '', 'danger', 'N', '0', NULL, NULL, NOW(), NULL, NOW(), NULL);

-- ac_language 字典数据
INSERT INTO `sys_dict_data` VALUES (120, '000000', 0, 'Java', 'java', 'ac_language', '', 'primary', 'N', '0', NULL, NULL, NOW(), NULL, NOW(), NULL);
INSERT INTO `sys_dict_data` VALUES (121, '000000', 1, 'Python', 'python', 'ac_language', '', 'success', 'N', '0', NULL, NULL, NOW(), NULL, NOW(), NULL);
