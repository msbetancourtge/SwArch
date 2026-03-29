package com.clickmunch.CheckoutService.dto;

import java.math.BigDecimal;

public record CheckoutResponse(
        Long orderId,
        Long reservationId,
        BigDecimal total,
        String status,
        String message
) {}
