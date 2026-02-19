package com.example.orderservice.service;

import org.springframework.stereotype.Service;

import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.state.OrderState;
import com.example.orderservice.state.OrderEvent;
import org.springframework.statemachine.StateMachineFactory;
import org.springframework.statemachine.StateMachine;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@Service
public class OrderService {

    public static final String ORDER_ID_HEADER = "order_id";

    private final OrderRepository orderRepository;
    private final StateMachineFactory<OrderState, OrderEvent> stateMachineFactory;

    public OrderService(OrderRepository orderRepository, StateMachineFactory<OrderState, OrderEvent> stateMachineFactory) {
        this.orderRepository = orderRepository;
        this.stateMachineFactory = stateMachineFactory;
    }

    public Order createOrder(Order order){
        order.setStatus(OrderState.ORDER_CREATED);
        Order saveOrder = orderRepository.save(order);

        StateMachine<OrderState, OrderEvent> stateMachine = stateMachineFactory.getStateMachine(saveOrder.getId());
        stateMachine.start();

        Message<OrderEvent> message = MessageBuilder
            .withPayload(OrderEvent.CREATE_ORDER)
            .setHeader(ORDER_ID_HEADER, saveOrder.getId())
            .build();
        stateMachine.sendEvent(message);
        
        return saveOrder;
    }

}
