package com.example.orderservice.service;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Bean;
import org.springframework.statemachine.action.Action;
import com.example.orderservice.entity.Order;
import com.example.orderservice.event.OrderEvent;
import com.example.orderservice.state.OrderState;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import com.example.orderservice.repository.OrderRepository;

import java.util.UUID;
import org.springframework.messaging.support.MessageBuilder;

@Configuration
public class SagaActions {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private final String PAYMENT_SERVICE_URL = "http://payment-service:8081/payment";
    private final String INVENTORY_SERVICE_URL = "http://inventory-service:8082/inventory";

    public SagaActions(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Bean
    public Action<OrderState, OrderEvent> processPaymentAction() {
        return context -> {
            UUID orderId = (UUID) context.getMessage().getHeaders().get(OrderService.ORDER_ID_HEADER);
            Order order = orderRepository.findById(orderId).orElseThrow();

            System.out.println("Saga Action: Processing Payment for Order: " + orderId);

            try {

                String url = PAYMENT_SERVICE_URL + "?orderId=" + orderId + "&amount=" + order.getAmount();
                restTemplate.postForEntity(url, null, String.class);

                context.getStateMachine().sendEvent(MessageBuilder
                        .withPayload(OrderEvent.PAYMENT_SUCCESS)
                        .setHeader(OrderService.ORDER_ID_HEADER, orderId)
                        .build());

            } catch (Exception e) {

                System.err.println("Payment Failed for Order: " + orderId + " Error: " + e.getMessage());

                context.getStateMachine().sendEvent(MessageBuilder
                        .withPayload(OrderEvent.PAYMENT_FAILURE)
                        .setHeader(OrderService.ORDER_ID_HEADER, orderId)
                        .build());
            }

        };
    }

    @Bean
    public Action<OrderState, OrderEvent> reserveInventoryAction() {
        return context -> {
            UUID orderId = (UUID) context.getMessage().getHeaders().get(OrderService.ORDER_ID_HEADER);
            Order order = orderRepository.findById(orderId).orElseThrow();

            System.out.println("Saga Action: Reserving Inventory for Order: " + orderId);

            try {

            
                String url = INVENTORY_SERVICE_URL + "/reserve?orderId=" + orderId + "&quantity=" + order.getQuantity();
                restTemplate.postForEntity(url, null, String.class);

                context.getStateMachine().sendEvent(MessageBuilder
                        .withPayload(OrderEvent.INVENTORY_SUCCESS)
                        .setHeader(OrderService.ORDER_ID_HEADER, orderId)
                        .build());

            } catch (Exception e) {

                System.err.println("Inventory Reservation Failed for Order: " + orderId + " Error: " + e.getMessage());

                context.getStateMachine().sendEvent(MessageBuilder
                        .withPayload(OrderEvent.INVENTORY_FAILURE)
                        .setHeader(OrderService.ORDER_ID_HEADER, orderId)
                        .build());
            }

        };
    }

    @Bean
    public Action<OrderState, OrderEvent> compensatePaymentAction() {
        return context -> {
            UUID orderId = (UUID) context.getMessage().getHeaders().get(OrderService.ORDER_ID_HEADER);
            
            System.out.println("Saga Action: Compensating Payment for Order: " + orderId);

            try {
                String url = PAYMENT_SERVICE_URL + "/cancel?orderId=" + orderId;
                restTemplate.postForEntity(url, null, String.class);

            } catch (Exception e) {
                System.err.println("Payment Compensation Failed for Order: " + orderId + " Error: " + e.getMessage());
            }

        };
    }

    @Bean
    public Action<OrderState, OrderEvent> compensateInventoryAction() {
        return context -> {
            UUID orderId = (UUID) context.getMessage().getHeaders().get(OrderService.ORDER_ID_HEADER);

            System.out.println("Saga Action: Compensating Inventory for Order: " + orderId);

            try {
        
                String url = INVENTORY_SERVICE_URL + "/release?orderId=" + orderId;
                restTemplate.postForEntity(url, null, String.class);

            } catch (Exception e) {
                System.err.println("Inventory Compensation Failed for Order: " + orderId + " Error: " + e.getMessage());
            }

        };
    }

}
