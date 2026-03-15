package com.clickmunch.RestaurantService.dto;

import java.util.List;

public record RestaurantDetailsResponse(
        Long id,
        String name,
        String address,
        Double latitude,
        Double longitude,
        String description,
        List<MenuCategoryResponse> menuCategories
) {
}
