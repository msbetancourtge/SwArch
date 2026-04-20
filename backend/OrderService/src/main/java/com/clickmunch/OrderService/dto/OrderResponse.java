package com.clickmunch.OrderService.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        String customerName,
        Long restaurantId,
        String restaurantName,
        String status,
        String channel,
        String notes,
        String eta,
        BigDecimal total,
        Long tableId,
        Long waiterId,
        BigDecimal tipAmount,
        String waiterComment,
        Integer preparationMinutes,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
