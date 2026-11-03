package com.campus.errand.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置（访问 /api/swagger-ui.html）
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI errandOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("校园跑腿任务发布与接单系统 API")
                .description("任务发布 / 接单 / 结算 / 评价 四大核心模块接口文档")
                .version("1.0.0")
                .contact(new Contact().name("Campus Errand")));
    }
}
