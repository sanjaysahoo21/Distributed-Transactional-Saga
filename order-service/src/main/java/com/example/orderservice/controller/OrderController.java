package com.example.orderservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.entity.Order;
import com.example.orderservice.state.OrderState;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order createdOrder = orderService.createOrder(order);

        if (createdOrder.getStatus() == OrderState.ORDER_FAILED) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createdOrder);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }
}
