package com.clickmunch.MenuService.repository;

import com.clickmunch.MenuService.entity.MenuItem;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuItemRepository extends ListCrudRepository<MenuItem, Long> {

    // Retrieve items for a category, ordered by name
    @Query("SELECT * FROM menu_items WHERE category_id = :categoryId ORDER BY name")
    List<MenuItem> findByCategoryId(@Param("categoryId") Long categoryId);

    // Bulk lookup by category ids
    List<MenuItem> findAllByCategoryIdIn(Collection<Long> categoryIds);

    // Optional helper to validate item belongs to a category
    @Query("SELECT * FROM menu_items WHERE id = :id AND category_id = :categoryId")
    Optional<MenuItem> findByIdAndCategoryId(@Param("id") Long id, @Param("categoryId") Long categoryId);

        // Delete all items for a set of category ids
    @Query("DELETE FROM menu_items WHERE category_id IN (:categoryIds)")
    void deleteAllByCategoryIdIn(@Param("categoryIds") Collection<Long> categoryIds);
}