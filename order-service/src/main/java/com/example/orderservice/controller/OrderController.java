package com.example.orderservice.controller;

import org.springframework.web.bind.annotation.*;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.entity.Order;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        return orderService.createOrder(order);
    }
}
