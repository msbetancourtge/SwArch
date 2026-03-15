package com.clickmunch.MenuService.entity;

import lombok.Data;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Menu Items represent individual food or drink offerings within a restaurant's menu. " +
        "Each menu item is associated with a specific category and includes details such as name, description, price," +
        " and image URL.")
@Data
@Table("menu_items")
public class MenuItem {
    @Schema(description = "Unique identifier for the menu item", example = "100")
    @Id
    private Long id;
    @Schema(description = "Identifier of the category this menu item belongs to", example = "1")
    @Column("category_id")
    private Long categoryId;
    @Schema(description = "Name of the menu item", example = "Cheeseburger")
    private String name;
    @Schema(description = "Description of the menu item", example = "A juicy grilled cheeseburger with lettuce, tomato," +
            " and pickles.")
    private String description;
    @Schema(description = "Price of the menu item", example = "9.99")
    private BigDecimal price;
    @Schema(description = "Image URL of the menu item", example = "http://example.com/images/cheeseburger.jpg")
    private String imageUrl;
}
