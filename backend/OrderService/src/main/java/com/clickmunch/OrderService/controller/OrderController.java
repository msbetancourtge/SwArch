package com.clickmunch.OrderService.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.clickmunch.OrderService.dto.AddItemsRequest;
import com.clickmunch.OrderService.dto.CreateOrderRequest;
import com.clickmunch.OrderService.dto.OrderResponse;
import com.clickmunch.OrderService.dto.TipRequest;
import com.clickmunch.OrderService.dto.UpdateOrderRequest;
import com.clickmunch.OrderService.dto.UpdateStatusRequest;
import com.clickmunch.OrderService.dto.WaiterCallRequest;
import com.clickmunch.OrderService.dto.WaiterCallResponse;
import com.clickmunch.OrderService.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByRestaurant(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderService.getOrdersByRestaurantId(restaurantId));
    }

    @GetMapping("/restaurant/{restaurantId}/status/{status}")
    public ResponseEntity<List<OrderResponse>> getOrdersByRestaurantAndStatus(
            @PathVariable Long restaurantId, @PathVariable String status) {
        return ResponseEntity.ok(orderService.getOrdersByRestaurantAndStatus(restaurantId, status));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomerId(customerId));
    }

    @GetMapping("/waiter/{waiterId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByWaiter(@PathVariable Long waiterId) {
        return ResponseEntity.ok(orderService.getOrdersByWaiterId(waiterId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(@PathVariable Long id, @RequestBody UpdateOrderRequest request) {
        return ResponseEntity.ok(orderService.updateOrder(id, request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<OrderResponse> addItemsToOrder(@PathVariable Long id,
                                                          @Valid @RequestBody AddItemsRequest request) {
        return ResponseEntity.ok(orderService.addItemsToOrder(id, request));
    }

    @PutMapping("/{id}/assign-waiter")
    public ResponseEntity<OrderResponse> assignWaiter(@PathVariable Long id, @RequestParam Long waiterId) {
        return ResponseEntity.ok(orderService.assignWaiter(id, waiterId));
    }

    @PutMapping("/{id}/assign-table")
    public ResponseEntity<OrderResponse> assignTable(@PathVariable Long id, @RequestParam Long tableId) {
        return ResponseEntity.ok(orderService.assignTable(id, tableId));
    }

    @PostMapping("/{id}/tip")
    public ResponseEntity<OrderResponse> addTip(@PathVariable Long id, @Valid @RequestBody TipRequest request) {
        return ResponseEntity.ok(orderService.addTip(id, request));
    }

    // ─── Waiter Calls ───

    @PostMapping("/waiter-call")
    public ResponseEntity<WaiterCallResponse> callWaiter(@Valid @RequestBody WaiterCallRequest request) {
        return ResponseEntity.ok(orderService.createWaiterCall(request));
    }

    @GetMapping("/waiter-calls/restaurant/{restaurantId}/pending")
    public ResponseEntity<List<WaiterCallResponse>> getPendingCalls(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderService.getPendingWaiterCalls(restaurantId));
    }

    @PutMapping("/waiter-calls/{callId}/acknowledge")
    public ResponseEntity<WaiterCallResponse> acknowledgeCall(@PathVariable Long callId) {
        return ResponseEntity.ok(orderService.acknowledgeWaiterCall(callId));
    }

    @PutMapping("/waiter-calls/{callId}/resolve")
    public ResponseEntity<WaiterCallResponse> resolveCall(@PathVariable Long callId) {
        return ResponseEntity.ok(orderService.resolveWaiterCall(callId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
