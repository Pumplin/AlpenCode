package org.ruoyi.common.core.constant;

/**
 * OJ 业务通用常量
 * @author 32846
 */
public interface CommonConstants {
    /** 未删除 */
    Integer NOT_DELETE = 0;
    /** 已删除（与框架 del_flag 保持一致：0=存在 2=删除） */
    Integer DELETED = 2;
    /** 正常/启用（与框架 sys_normal_disable 保持一致：0=正常） */
    Integer IS_AVAILABLE = 0;
    /** 停用（与框架 sys_normal_disable 保持一致：1=停用） */
    Integer NOT_AVAILABLE = 1;
}
