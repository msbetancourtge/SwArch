package com.clickmunch.OrderService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OrderDocsConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info().title("ClickAndMunch: Order Service")
                        .description("Order Service for the ClickAndMunch application. " +
                                "Handles order creation, status management, and order history.")
                        .version("0.1"));
    }
}
