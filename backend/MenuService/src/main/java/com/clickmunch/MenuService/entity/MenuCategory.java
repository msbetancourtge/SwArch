package com.clickmunch.MenuService.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Menu Categories are used to organize menu items within a restaurant's menu." +
        " Each category represents a specific type of food or drink, such as Appetizers, Main Courses, Desserts, or" +
        " Beverages.")
@Data
@Table("menu_categories")
public class MenuCategory {
    @Schema(description = "Unique identifier for the menu category", example = "1")
    @Id
    private Long id;
    @Schema(description = "Identifier of the restaurant this category belongs to", example = "10")
    @Column("restaurant_id")
    private Long restaurantId;
    @Schema(description = "Name of the menu category", example = "BEBIDA")
    private Category category;
}
