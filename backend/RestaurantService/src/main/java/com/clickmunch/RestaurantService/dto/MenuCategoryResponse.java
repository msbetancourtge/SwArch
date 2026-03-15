package com.clickmunch.RestaurantService.dto;

import java.util.List;

public record MenuCategoryResponse(
        Long id,
        String name,
        List<MenuItemResponse> menuItems
) {
}
