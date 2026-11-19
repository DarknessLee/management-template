package com.mgmt.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标在需要审计的 Controller 方法上，由 OperationLogAspect 记录操作日志。
 * 只标"会改变数据"的接口，查询接口不记录，否则日志量会被翻页请求淹没。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 业务模块，如：用户管理 */
    String module();

    /** 操作动作，如：删除用户 */
    String action();
}
