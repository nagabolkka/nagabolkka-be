package com.nagabolkka.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(API 문서) 설정
 *  - 로컬: http://localhost:8080/swagger-ui/index.html
 *  - 운영: https://api.nagabolkka.shop/swagger-ui/index.html
 */

@Configuration
public class OpenApiConfig {

    private static final String JWT = "JWT";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("나가볼까? API")
                        .description("나가볼까? 백엔드 API 명세서 입니다.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(JWT, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(JWT));
    }
}
