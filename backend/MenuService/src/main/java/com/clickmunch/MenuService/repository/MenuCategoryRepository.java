package com.clickmunch.MenuService.repository;

import com.clickmunch.MenuService.entity.MenuCategory;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MenuCategoryRepository extends ListCrudRepository<MenuCategory, Long> {

    // Find categories for a restaurant, ordered by name
    @Query("SELECT * FROM menu_categories WHERE restaurant_id = :restaurantId ORDER BY name")
    List<MenuCategory> findByRestaurantId(@Param("restaurantId") Long restaurantId);

    // Bulk lookup by restaurant ids
    List<MenuCategory> findAllByRestaurantIdIn(Collection<Long> restaurantIds);

    // Delete all categories for a restaurant (FK ON DELETE CASCADE will remove items)
    @Query("DELETE FROM menu_categories WHERE restaurant_id = :restaurantId")
    void deleteAllByRestaurantId(@Param("restaurantId") Long restaurantId);
}