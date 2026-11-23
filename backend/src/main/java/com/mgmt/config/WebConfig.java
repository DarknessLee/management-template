package com.mgmt.config;

import com.mgmt.security.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web 层装配：登录拦截器 + 跨域。
 * 开发时前端走 vite 代理，其实同源；CORS 只是为了让"直接访问 8080"也方便调试。
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                // /uploads/** 是静态资源：<img> 标签不会带 Authorization 头，必须放行
                // 登录前接口：验证码、登录本身和品牌配置不能要求 token
                // Swagger UI 与 OpenAPI 文档端点同样免鉴权
                .excludePathPatterns("/auth/login", "/auth/captcha", "/settings/public", "/error", "/uploads/**",
                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html");
    }

    /**
     * 把本地 uploads 目录映射为静态资源，头像 URL 形如 /api/uploads/avatar/xxx.png。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        // 目录不存在时 toUri() 不会补结尾斜杠，会被当成单个文件，导致所有请求 404
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
