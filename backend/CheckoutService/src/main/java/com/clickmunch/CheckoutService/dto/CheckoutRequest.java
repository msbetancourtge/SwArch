package com.clickmunch.CheckoutService.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull Long customerId,
        @NotBlank String customerName,
        @NotNull Long restaurantId,
        @NotBlank String restaurantName,
        @NotEmpty @Valid List<CartItemRequest> items,
        String channel,
        Long reservationId,
        String notes
) {}
