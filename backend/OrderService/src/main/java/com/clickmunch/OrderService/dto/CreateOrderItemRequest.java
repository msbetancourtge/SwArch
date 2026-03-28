package com.clickmunch.OrderService.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateOrderItemRequest(
        @NotBlank String itemName,
        @Min(1) Integer quantity,
        String notes
) {
}
