package com.mgmt.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标在 Controller 方法上，表示调用该接口需要指定权限标识。
 * 例：@RequirePermission("users:delete")
 * 校验逻辑在 AuthInterceptor 中完成，未标注则只要求登录。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /** 权限标识，需与 sys_role_permission.permission 中的值一致 */
    String value();
}
