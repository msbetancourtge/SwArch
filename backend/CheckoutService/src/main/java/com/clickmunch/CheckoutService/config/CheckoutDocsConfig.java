package com.clickmunch.CheckoutService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class CheckoutDocsConfig {

    @Bean
    public OpenAPI checkoutOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Click & Munch - Checkout Service API")
                        .description("Saga Orchestrator — validates cart, creates orders, and links reservations")
                        .version("1.0.0"));
    }
}
