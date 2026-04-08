package com.clickmunch.OrderService.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clickmunch.OrderService.dto.AddItemsRequest;
import com.clickmunch.OrderService.dto.CreateOrderRequest;
import com.clickmunch.OrderService.dto.OrderItemResponse;
import com.clickmunch.OrderService.dto.OrderResponse;
import com.clickmunch.OrderService.dto.TipRequest;
import com.clickmunch.OrderService.dto.UpdateOrderRequest;
import com.clickmunch.OrderService.dto.UpdateStatusRequest;
import com.clickmunch.OrderService.dto.WaiterCallRequest;
import com.clickmunch.OrderService.dto.WaiterCallResponse;
import com.clickmunch.OrderService.entity.Order;
import com.clickmunch.OrderService.entity.OrderChannel;
import com.clickmunch.OrderService.entity.OrderItem;
import com.clickmunch.OrderService.entity.OrderStatus;
import com.clickmunch.OrderService.entity.WaiterCall;
import com.clickmunch.OrderService.repository.OrderItemRepository;
import com.clickmunch.OrderService.repository.OrderRepository;
import com.clickmunch.OrderService.repository.WaiterCallRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final WaiterCallRepository waiterCallRepository;

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
                .tableId(request.tableId())
                .waiterId(request.waiterId())
                .preparationMinutes(request.preparationMinutes())
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

    public List<OrderResponse> getOrdersByWaiterId(Long waiterId) {
        List<Order> orders = orderRepository.findByWaiterId(waiterId);
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
    public OrderResponse addItemsToOrder(Long id, AddItemsRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        List<OrderItem> newItems = request.items().stream()
                .map(itemReq -> OrderItem.builder()
                        .orderId(id)
                        .menuItemId(itemReq.menuItemId())
                        .productName(itemReq.productName())
                        .quantity(itemReq.quantity())
                        .unitPrice(itemReq.unitPrice())
                        .subtotal(itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity())))
                        .build())
                .toList();

        orderItemRepository.saveAll(newItems);

        // Recalculate total
        List<OrderItem> allItems = orderItemRepository.findByOrderId(id);
        BigDecimal newTotal = allItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotal(newTotal);
        order.setUpdatedAt(LocalDateTime.now());

        Order updated = orderRepository.save(order);
        return toResponse(updated, allItems);
    }

    @Transactional
    public OrderResponse assignWaiter(Long orderId, Long waiterId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.setWaiterId(waiterId);
        order.setUpdatedAt(LocalDateTime.now());
        Order updated = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return toResponse(updated, items);
    }

    @Transactional
    public OrderResponse assignTable(Long orderId, Long tableId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.setTableId(tableId);
        order.setUpdatedAt(LocalDateTime.now());
        Order updated = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return toResponse(updated, items);
    }

    @Transactional
    public OrderResponse addTip(Long orderId, TipRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.setTipAmount(request.tipAmount());
        if (request.waiterComment() != null) {
            order.setWaiterComment(request.waiterComment());
        }
        order.setUpdatedAt(LocalDateTime.now());
        Order updated = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return toResponse(updated, items);
    }

    // ─── Waiter Calls ───

    @Transactional
    public WaiterCallResponse createWaiterCall(WaiterCallRequest request) {
        WaiterCall call = WaiterCall.builder()
                .orderId(request.orderId())
                .tableId(request.tableId())
                .restaurantId(request.restaurantId())
                .status("PENDING")
                .message(request.message())
                .createdAt(LocalDateTime.now())
                .build();
        WaiterCall saved = waiterCallRepository.save(call);
        return toCallResponse(saved);
    }

    public List<WaiterCallResponse> getPendingWaiterCalls(Long restaurantId) {
        return waiterCallRepository.findPendingByRestaurantId(restaurantId).stream()
                .map(this::toCallResponse).toList();
    }

    @Transactional
    public WaiterCallResponse resolveWaiterCall(Long callId) {
        WaiterCall call = waiterCallRepository.findById(callId)
                .orElseThrow(() -> new IllegalArgumentException("Waiter call not found: " + callId));
        call.setStatus("RESOLVED");
        call.setResolvedAt(LocalDateTime.now());
        return toCallResponse(waiterCallRepository.save(call));
    }

    @Transactional
    public WaiterCallResponse acknowledgeWaiterCall(Long callId) {
        WaiterCall call = waiterCallRepository.findById(callId)
                .orElseThrow(() -> new IllegalArgumentException("Waiter call not found: " + callId));
        call.setStatus("ACKNOWLEDGED");
        return toCallResponse(waiterCallRepository.save(call));
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
                order.getTableId(),
                order.getWaiterId(),
                order.getTipAmount(),
                order.getWaiterComment(),
                order.getPreparationMinutes(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private WaiterCallResponse toCallResponse(WaiterCall call) {
        return new WaiterCallResponse(
                call.getId(), call.getOrderId(), call.getTableId(),
                call.getRestaurantId(), call.getStatus(), call.getMessage(),
                call.getCreatedAt(), call.getResolvedAt()
        );
    }

    private OrderChannel parseChannel(String channel) {
        if ("In-person".equalsIgnoreCase(channel) || "InPerson".equalsIgnoreCase(channel)) {
            return OrderChannel.InPerson;
        }
        return OrderChannel.Reservation;
    }
}
