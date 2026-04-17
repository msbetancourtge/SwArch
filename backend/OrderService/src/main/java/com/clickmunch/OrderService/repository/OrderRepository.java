package com.clickmunch.OrderService.repository;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import com.clickmunch.OrderService.entity.Order;

public interface OrderRepository extends ListCrudRepository<Order, Long> {

    @Query("SELECT * FROM orders WHERE restaurant_id = :restaurantId ORDER BY created_at DESC")
    List<Order> findByRestaurantId(@Param("restaurantId") Long restaurantId);

    @Query("SELECT * FROM orders WHERE customer_id = :customerId ORDER BY created_at DESC")
    List<Order> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT * FROM orders WHERE status = :status ORDER BY created_at DESC")
    List<Order> findByStatus(@Param("status") String status);

    @Query("SELECT * FROM orders WHERE restaurant_id = :restaurantId AND status = :status ORDER BY created_at DESC")
    List<Order> findByRestaurantIdAndStatus(@Param("restaurantId") Long restaurantId, @Param("status") String status);

    @Query("SELECT * FROM orders ORDER BY created_at DESC")
    List<Order> findAllOrderedByDate();
}
