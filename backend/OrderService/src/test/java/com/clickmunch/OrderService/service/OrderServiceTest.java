package com.clickmunch.OrderService.service;

import com.clickmunch.OrderService.dto.*;
import com.clickmunch.OrderService.entity.Order;
import com.clickmunch.OrderService.entity.OrderChannel;
import com.clickmunch.OrderService.entity.OrderItem;
import com.clickmunch.OrderService.entity.OrderStatus;
import com.clickmunch.OrderService.repository.OrderItemRepository;
import com.clickmunch.OrderService.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private OrderItem testItem;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .id(1L)
                .customerId(10L)
                .customerName("John Doe")
                .restaurantId(20L)
                .restaurantName("Test Restaurant")
                .status(OrderStatus.Preparing)
                .channel(OrderChannel.InPerson)
                .notes("No onions")
                .eta("15 min")
                .total(new BigDecimal("25.50"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testItem = OrderItem.builder()
                .id(1L)
                .orderId(1L)
                .menuItemId("item123")
                .productName("Burger")
                .quantity(2)
                .unitPrice(new BigDecimal("12.75"))
                .subtotal(new BigDecimal("25.50"))
                .build();
    }

    @Test
    void shouldCreateOrder() {
        CreateOrderRequest.OrderItemRequest itemReq = new CreateOrderRequest.OrderItemRequest(
                "item123", "Burger", 2, new BigDecimal("12.75"));
        CreateOrderRequest request = new CreateOrderRequest(
                10L, "John Doe", 20L, "Test Restaurant",
                "In-person", "No onions", "15 min", null, List.of(itemReq));

        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderItemRepository.saveAll(anyList())).thenReturn(List.of(testItem));

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("John Doe", response.customerName());
        assertEquals("Preparing", response.status());
        assertEquals(1, response.items().size());
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(anyList());
    }

    @Test
    void shouldGetOrderById() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(testItem));

        OrderResponse response = orderService.getOrderById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Test Restaurant", response.restaurantName());
    }

    @Test
    void shouldThrowWhenOrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> orderService.getOrderById(999L));
    }

    @Test
    void shouldGetOrdersByRestaurantId() {
        when(orderRepository.findByRestaurantId(20L)).thenReturn(List.of(testOrder));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(testItem));

        List<OrderResponse> responses = orderService.getOrdersByRestaurantId(20L);

        assertEquals(1, responses.size());
        assertEquals(20L, responses.get(0).restaurantId());
    }

    @Test
    void shouldUpdateOrderStatus() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(testItem));

        UpdateStatusRequest request = new UpdateStatusRequest("Ready");
        OrderResponse response = orderService.updateOrderStatus(1L, request);

        assertNotNull(response);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldUpdateOrderNotes() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(testItem));

        UpdateOrderRequest request = new UpdateOrderRequest("Extra sauce", "20 min");
        OrderResponse response = orderService.updateOrder(1L, request);

        assertNotNull(response);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldDeleteOrder() {
        when(orderRepository.existsById(1L)).thenReturn(true);

        orderService.deleteOrder(1L);

        verify(orderItemRepository).deleteByOrderId(1L);
        verify(orderRepository).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentOrder() {
        when(orderRepository.existsById(999L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> orderService.deleteOrder(999L));
    }
}
