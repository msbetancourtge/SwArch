package com.clickmunch.RestaurantService.dto;

public record CreateRestaurantRequest(
        Long ownerId,
        String name,
        String description,
        String phone,
        String email,
        Double latitude,
        Double longitude
) {
}
