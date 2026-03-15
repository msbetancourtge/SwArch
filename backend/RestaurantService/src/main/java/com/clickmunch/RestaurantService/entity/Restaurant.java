package com.clickmunch.RestaurantService.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("restaurants")
public class Restaurant {

    @Id
    private Long id;
    private Long ownerId;
    private String name;
    private String description;
    private String phone;
    private String email;
    private Long locationId;
}
