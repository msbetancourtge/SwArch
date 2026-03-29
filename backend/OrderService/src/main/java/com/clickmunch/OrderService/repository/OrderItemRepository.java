package com.clickmunch.OrderService.repository;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;

import com.clickmunch.OrderService.entity.OrderItem;

public interface OrderItemRepository extends ListCrudRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);
}
