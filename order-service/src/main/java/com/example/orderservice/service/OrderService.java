package com.example.orderservice.service;

import org.springframework.stereotype.Service;

import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.state.OrderState;
import com.example.orderservice.state.OrderEvent;
import com.example.orderservice.config.OrderStateMachineInterceptor;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.StateMachine;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;

@Service
public class OrderService {

        public static final String ORDER_ID_HEADER = "order_id";

        private final OrderRepository orderRepository;
        private final StateMachineFactory<OrderState, OrderEvent> stateMachineFactory;
        private final OrderStateMachineInterceptor orderStateMachineInterceptor;

        public OrderService(OrderRepository orderRepository,
                        StateMachineFactory<OrderState, OrderEvent> stateMachineFactory,
                        OrderStateMachineInterceptor orderStateMachineInterceptor) {
                this.orderRepository = orderRepository;
                this.stateMachineFactory = stateMachineFactory;
                this.orderStateMachineInterceptor = orderStateMachineInterceptor;
        }

        public Order createOrder(Order order) {
                order.setStatus(OrderState.ORDER_CREATED);
                Order saveOrder = orderRepository.save(order);

                StateMachine<OrderState, OrderEvent> stateMachine = stateMachineFactory
                                .getStateMachine(saveOrder.getId().toString());

                // Register the interceptor on the state machine instance (Spring SM 4.x way)
                stateMachine.getStateMachineAccessor()
                                .doWithAllRegions(accessor -> accessor
                                                .addStateMachineInterceptor(orderStateMachineInterceptor));

                // Use reactive start for SM 4.x
                stateMachine.startReactively().block();

                Message<OrderEvent> message = MessageBuilder
                                .withPayload(OrderEvent.CREATE_ORDER)
                                .setHeader(ORDER_ID_HEADER, saveOrder.getId())
                                .build();
                stateMachine.sendEvent(Mono.just(message)).blockLast();

                // Re-fetch from DB to get the latest status after saga completion
                return orderRepository.findById(saveOrder.getId()).orElse(saveOrder);
        }

}
