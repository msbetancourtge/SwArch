package com.clickmunch.OrderService.entity;

public enum OrderStatus {
    Pending,
    SentToKitchen,
    Preparing,
    Ready,
    Served,
    Delivered,
    Cancelled
}
