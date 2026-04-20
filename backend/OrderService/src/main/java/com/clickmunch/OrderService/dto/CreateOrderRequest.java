package com.clickmunch.OrderService.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotNull Long customerId,
        @NotBlank String customerName,
        @NotNull Long restaurantId,
        @NotBlank String restaurantName,
        @NotBlank String channel,
        String notes,
        String eta,
        Long reservationId,
        Long tableId,
        Long waiterId,
        Integer preparationMinutes,
        @NotNull @Size(min = 1) List<@Valid OrderItemRequest> items
) {
    public record OrderItemRequest(
            @NotBlank String menuItemId,
            @NotBlank String productName,
            @NotNull @Positive Integer quantity,
            @NotNull @Positive BigDecimal unitPrice
    ) {}
}
