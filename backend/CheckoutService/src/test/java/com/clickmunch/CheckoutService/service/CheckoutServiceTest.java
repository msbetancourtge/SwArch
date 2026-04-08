package com.clickmunch.CheckoutService.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.clickmunch.CheckoutService.client.MenuClient;
import com.clickmunch.CheckoutService.client.OrderClient;
import com.clickmunch.CheckoutService.client.ReservationClient;
import com.clickmunch.CheckoutService.dto.CartItemRequest;
import com.clickmunch.CheckoutService.dto.CheckoutRequest;
import com.clickmunch.CheckoutService.dto.CheckoutResponse;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private MenuClient menuClient;

    @Mock
    private OrderClient orderClient;

    @Mock
    private ReservationClient reservationClient;

    @InjectMocks
    private CheckoutService checkoutService;

    private CheckoutRequest buildRequest(Long reservationId) {
        return new CheckoutRequest(
                10L, "John Doe", 5L, "La Parrilla",
                List.of(
                        new CartItemRequest("item1", "Burger", 2, new BigDecimal("9.99")),
                        new CartItemRequest("item2", "Fries", 1, new BigDecimal("4.50"))
                ),
                "InPerson",
                reservationId,
                "No onions",
                "CARD"
        );
    }

    @Test
    void processCheckout_withoutReservation_shouldCreateOrder() {
        CheckoutRequest request = buildRequest(null);

        Map<String, Object> orderResponse = Map.of(
                "id", 42,
                "status", "Preparing",
                "total", 24.48
        );
        when(orderClient.createOrder(any())).thenReturn(orderResponse);

        CheckoutResponse response = checkoutService.processCheckout(request);

        assertEquals(42L, response.orderId());
        assertNull(response.reservationId());
        assertEquals(new BigDecimal("24.48"), response.total());
        assertEquals("Preparing", response.status());
        verify(orderClient).createOrder(any());
        verifyNoInteractions(reservationClient);
    }

    @Test
    void processCheckout_withReservation_shouldLinkOrder() {
        CheckoutRequest request = buildRequest(100L);

        when(reservationClient.getReservation(100L)).thenReturn(Map.of("id", 100, "status", "Confirmada"));

        Map<String, Object> orderResponse = Map.of(
                "id", 42,
                "status", "Preparing",
                "total", 24.48
        );
        when(orderClient.createOrder(any())).thenReturn(orderResponse);
        when(reservationClient.linkOrder(eq(100L), eq(42L))).thenReturn(Map.of("orderId", 42));

        CheckoutResponse response = checkoutService.processCheckout(request);

        assertEquals(42L, response.orderId());
        assertEquals(100L, response.reservationId());
        verify(reservationClient).getReservation(100L);
        verify(reservationClient).linkOrder(100L, 42L);
    }

    @Test
    void processCheckout_reservationNotFound_shouldThrow() {
        CheckoutRequest request = buildRequest(999L);

        when(reservationClient.getReservation(999L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> checkoutService.processCheckout(request));
        verifyNoInteractions(orderClient);
    }

    @Test
    void processCheckout_orderCreationFails_shouldThrow() {
        CheckoutRequest request = buildRequest(null);

        when(orderClient.createOrder(any())).thenThrow(new RuntimeException("Connection refused"));

        assertThrows(RuntimeException.class,
                () -> checkoutService.processCheckout(request));
    }

    @Test
    void processCheckout_totalCalculation_shouldBeCorrect() {
        CheckoutRequest request = new CheckoutRequest(
                10L, "Jane", 5L, "Sushi Place",
                List.of(
                        new CartItemRequest("a", "Roll A", 3, new BigDecimal("12.00")),
                        new CartItemRequest("b", "Roll B", 2, new BigDecimal("15.50"))
                ),
                "Reservation", null, null, null
        );

        when(orderClient.createOrder(any())).thenReturn(Map.of("id", 1, "status", "Preparing"));

        CheckoutResponse response = checkoutService.processCheckout(request);

        // (3 * 12.00) + (2 * 15.50) = 36.00 + 31.00 = 67.00
        assertEquals(new BigDecimal("67.00"), response.total());
    }
}
