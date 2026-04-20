package com.clickmunch.CheckoutService.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OrderClient {

    private final RestClient restClient;

    public OrderClient(@Value("${services.order.url}") String orderServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(orderServiceUrl)
                .build();
    }

    /**
     * Creates an order in the OrderService and returns the response.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createOrder(Map<String, Object> orderRequest) {
        return restClient.post()
                .uri("/api/orders")
                .header("Content-Type", "application/json")
                .body(orderRequest)
                .retrieve()
                .body(Map.class);
    }
}
