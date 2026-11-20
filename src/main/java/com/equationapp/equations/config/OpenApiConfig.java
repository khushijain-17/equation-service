package com.equationapp.equations.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("Equation Service API")
                .version("1.0.0")
                .description("Store algebraic equations as postfix expression trees, list them back in infix "
                        + "form and evaluate them with variable values."));
    }
}
