package com.clickmunch.CheckoutService.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ReservationClient {

    private final RestClient restClient;

    public ReservationClient(@Value("${services.reservation.url}") String reservationServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(reservationServiceUrl)
                .build();
    }

    /**
     * Links an order to an existing reservation.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> linkOrder(Long reservationId, Long orderId) {
        return restClient.put()
                .uri("/api/reservations/{id}/link-order", reservationId)
                .header("Content-Type", "application/json")
                .body(Map.of("orderId", orderId))
                .retrieve()
                .body(Map.class);
    }

    /**
     * Verifies a reservation exists and returns its data.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getReservation(Long reservationId) {
        try {
            return restClient.get()
                    .uri("/api/reservations/{id}", reservationId)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            return null;
        }
    }
}
