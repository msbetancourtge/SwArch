package com.clickmunch.CheckoutService.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MenuClient {

    private final RestClient restClient;

    public MenuClient(@Value("${services.menu.url}") String menuServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(menuServiceUrl)
                .build();
    }

    /**
     * Verifies a menu item exists and returns its data.
     * Returns null if the item is not found.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMenuItem(String restaurantId, String menuItemId) {
        try {
            return restClient.get()
                    .uri("/api/menus/{restaurantId}/items/{itemId}", restaurantId, menuItemId)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            return null;
        }
    }
}
