package com.clickmunch.OrderService.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("orders")
public class Order {
    @Id
    private Long id;
    private Long customerId;
    private String customerName;
    private Long restaurantId;
    private String restaurantName;
    private OrderStatus status;
    private OrderChannel channel;
    private String notes;
    private String eta;
    private BigDecimal total;
    private Long tableId;
    private Long waiterId;
    private BigDecimal tipAmount;
    private String waiterComment;
    private Integer preparationMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
