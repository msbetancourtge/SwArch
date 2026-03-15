package com.clickmunch.RestaurantService.dto;

public record MenuItemResponse(
        Long id,
        Long categoryId,
        String name,
        String description,
        Double price,
        String imageUrl
) {
}
