package com.clickmunch.MenuService.controller;

import com.clickmunch.MenuService.dto.MenuCategoryRequest;
import com.clickmunch.MenuService.dto.MenuCreateRequest;
import com.clickmunch.MenuService.dto.MenuItemRequest;
import com.clickmunch.MenuService.entity.MenuCategory;
import com.clickmunch.MenuService.entity.MenuItem;
import com.clickmunch.MenuService.service.MenuService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MenuItemController.class, MenuCategoryController.class})
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MenuService menuService;

    @Test
    void createCategory_returnsCreated() throws Exception {
        Mockito.when(menuService.createMenuCategory(Mockito.any(MenuCategoryRequest.class)))
                .thenReturn(new MenuCategory(1L, 1L, "Main", "DESC"));

        mockMvc.perform(post("/api/menus/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"restaurantId\":1,\"name\":\"Main\",\"description\":\"DESC\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void createItem_returnsCreated() throws Exception {
        Mockito.when(menuService.createMenuItem(Mockito.eq(1L), Mockito.any(MenuItemRequest.class)))
                .thenReturn(new MenuItem(1L, 1L, "Burger", "Tasty", 9.99));

        mockMvc.perform(post("/api/menus/categories/1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Burger\",\"description\":\"Tasty\",\"price\":9.99}"))
                .andExpect(status().isCreated());
    }

    @Test
    void getMenuByRestaurant_returnsOk() throws Exception {
        Mockito.when(menuService.getMenuByRestaurantId(1L)).thenReturn(null);
        mockMvc.perform(get("/api/menus/restaurants/1"))
                .andExpect(status().isOk());
    }

}
