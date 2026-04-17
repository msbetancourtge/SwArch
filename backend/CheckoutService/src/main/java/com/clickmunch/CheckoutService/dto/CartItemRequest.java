package com.clickmunch.CheckoutService.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CartItemRequest(
        @NotBlank String menuItemId,
        @NotBlank String productName,
        @NotNull @Min(1) Integer quantity,
        @NotNull BigDecimal unitPrice
) {}
