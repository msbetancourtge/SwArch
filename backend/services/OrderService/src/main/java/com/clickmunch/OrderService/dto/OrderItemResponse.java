package com.clickmunch.OrderService.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        String menuItemId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
