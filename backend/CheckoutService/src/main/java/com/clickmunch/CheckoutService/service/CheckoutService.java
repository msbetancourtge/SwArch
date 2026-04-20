package com.clickmunch.CheckoutService.service;

import com.clickmunch.CheckoutService.client.MenuClient;
import com.clickmunch.CheckoutService.client.OrderClient;
import com.clickmunch.CheckoutService.client.ReservationClient;
import com.clickmunch.CheckoutService.dto.CartItemRequest;
import com.clickmunch.CheckoutService.dto.CheckoutRequest;
import com.clickmunch.CheckoutService.dto.CheckoutResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final MenuClient menuClient;
    private final OrderClient orderClient;
    private final ReservationClient reservationClient;

    public CheckoutResponse processCheckout(CheckoutRequest request) {
        // Step 1: Validate reservation if provided
        if (request.reservationId() != null) {
            Map<String, Object> reservation = reservationClient.getReservation(request.reservationId());
            if (reservation == null) {
                throw new IllegalArgumentException("Reservation not found: " + request.reservationId());
            }
        }

        // Step 2: Build order items and calculate total
        BigDecimal total = BigDecimal.ZERO;
        List<Map<String, Object>> orderItems = new ArrayList<>();

        for (CartItemRequest item : request.items()) {
            BigDecimal subtotal = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            total = total.add(subtotal);

            Map<String, Object> orderItem = new HashMap<>();
            orderItem.put("menuItemId", item.menuItemId());
            orderItem.put("productName", item.productName());
            orderItem.put("quantity", item.quantity());
            orderItem.put("unitPrice", item.unitPrice());
            orderItems.add(orderItem);
        }

        // Step 3: Create order via OrderService
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("customerId", request.customerId());
        orderRequest.put("customerName", request.customerName());
        orderRequest.put("restaurantId", request.restaurantId());
        orderRequest.put("restaurantName", request.restaurantName());
        orderRequest.put("items", orderItems);
        orderRequest.put("channel", request.channel() != null ? request.channel() : "InPerson");
        if (request.notes() != null) {
            orderRequest.put("notes", request.notes());
        }

        Map<String, Object> orderResponse;
        try {
            orderResponse = orderClient.createOrder(orderRequest);
        } catch (Exception e) {
            log.error("Failed to create order: {}", e.getMessage());
            throw new RuntimeException("Failed to create order. Please try again.");
        }

        Long orderId = ((Number) orderResponse.get("id")).longValue();

        // Step 4: Link order to reservation if applicable
        Long reservationId = request.reservationId();
        if (reservationId != null) {
            try {
                reservationClient.linkOrder(reservationId, orderId);
            } catch (Exception e) {
                log.warn("Failed to link order {} to reservation {}: {}",
                        orderId, reservationId, e.getMessage());
            }
        }

        return new CheckoutResponse(
                orderId,
                reservationId,
                total,
                "Preparing",
                "Order placed successfully",
                request.paymentMethod() != null ? request.paymentMethod() : "CASH"
        );
    }
}
