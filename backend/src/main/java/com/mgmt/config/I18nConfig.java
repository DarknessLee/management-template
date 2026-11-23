package com.mgmt.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * 接口提示信息的国际化配置：
 * 默认文件 messages.properties 为中文，messages_en.properties 为英文，
 * 未匹配到对应语言时回退到默认文件。
 */
@Configuration
public class I18nConfig {

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        // 跟随 Accept-Language，不跟随 JVM 默认语言，避免服务器环境改变输出语言
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
