package com.mgmt.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置：统一标题、版本与 JWT Bearer 鉴权。
 * 全局默认要求鉴权，登录/验证码等公开接口在 Controller 上用 @SecurityRequirements 取消。
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI mgmtOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("后台管理系统 API")
                        .version("1.0.0")
                        .description("登录后在右上角 Authorize 中填入 JWT token（无需 Bearer 前缀）即可调试需要鉴权的接口。"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
