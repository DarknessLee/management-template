package com.mgmt.common;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * 业务层获取国际化提示的便捷入口。
 * Locale 取自当前请求的 Accept-Language（由 DispatcherServlet 写入 LocaleContextHolder），
 * 因此 Service / 拦截器里直接调用即可，不用逐层传递 Locale。
 */
@Component
@RequiredArgsConstructor
public class I18n implements InitializingBean {

    private final MessageSource messageSource;
    private static MessageSource source;

    @Override
    public void afterPropertiesSet() {
        source = messageSource;
    }

    public static String get(String code, Object... args) {
        return source.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
