package com.clickmunch.OrderService.service;

import com.clickmunch.OrderService.dto.*;
import com.clickmunch.OrderService.entity.Order;
import com.clickmunch.OrderService.entity.OrderChannel;
import com.clickmunch.OrderService.entity.OrderItem;
import com.clickmunch.OrderService.entity.OrderStatus;
import com.clickmunch.OrderService.repository.OrderItemRepository;
import com.clickmunch.OrderService.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        BigDecimal total = request.items().stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrderChannel channel = parseChannel(request.channel());

        Order order = Order.builder()
                .customerId(request.customerId())
                .customerName(request.customerName())
                .restaurantId(request.restaurantId())
                .restaurantName(request.restaurantName())
                .status(OrderStatus.Preparing)
                .channel(channel)
                .notes(request.notes())
                .eta(request.eta())
                .total(total)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> items = request.items().stream()
                .map(itemReq -> OrderItem.builder()
                        .orderId(savedOrder.getId())
                        .menuItemId(itemReq.menuItemId())
                        .productName(itemReq.productName())
                        .quantity(itemReq.quantity())
                        .unitPrice(itemReq.unitPrice())
                        .subtotal(itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity())))
                        .build())
                .toList();

        List<OrderItem> savedItems = orderItemRepository.saveAll(items);
        return toResponse(savedOrder, savedItems);
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        return toResponse(order, items);
    }

    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAllOrderedByDate();
        return orders.stream().map(order -> {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            return toResponse(order, items);
        }).toList();
    }

    public List<OrderResponse> getOrdersByRestaurantId(Long restaurantId) {
        List<Order> orders = orderRepository.findByRestaurantId(restaurantId);
        return orders.stream().map(order -> {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            return toResponse(order, items);
        }).toList();
    }

    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        return orders.stream().map(order -> {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            return toResponse(order, items);
        }).toList();
    }

    public List<OrderResponse> getOrdersByRestaurantAndStatus(Long restaurantId, String status) {
        List<Order> orders = orderRepository.findByRestaurantIdAndStatus(restaurantId, status);
        return orders.stream().map(order -> {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            return toResponse(order, items);
        }).toList();
    }

    @Transactional
    public OrderResponse updateOrder(Long id, UpdateOrderRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        if (request.notes() != null) {
            order.setNotes(request.notes());
        }
        if (request.eta() != null) {
            order.setEta(request.eta());
        }
        order.setUpdatedAt(LocalDateTime.now());

        Order updated = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        return toResponse(updated, items);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateStatusRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        OrderStatus newStatus = OrderStatus.valueOf(request.status());
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        Order updated = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        return toResponse(updated, items);
    }

    @Transactional
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new IllegalArgumentException("Order not found: " + id);
        }
        orderItemRepository.deleteByOrderId(id);
        orderRepository.deleteById(id);
    }

    private OrderResponse toResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getMenuItemId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                )).toList();

        String channelStr = order.getChannel() == OrderChannel.InPerson ? "In-person" : "Reservation";

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getCustomerName(),
                order.getRestaurantId(),
                order.getRestaurantName(),
                order.getStatus().name(),
                channelStr,
                order.getNotes(),
                order.getEta(),
                order.getTotal(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderChannel parseChannel(String channel) {
        if ("In-person".equalsIgnoreCase(channel) || "InPerson".equalsIgnoreCase(channel)) {
            return OrderChannel.InPerson;
        }
        return OrderChannel.Reservation;
    }
}
