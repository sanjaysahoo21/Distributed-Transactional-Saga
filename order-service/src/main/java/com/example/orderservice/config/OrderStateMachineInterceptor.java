package com.example.orderservice.config;

import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;

import com.example.orderservice.state.OrderEvent;
import com.example.orderservice.state.OrderState;
import com.example.orderservice.service.OrderService;
import org.springframework.messaging.Message;
import org.springframework.statemachine.transition.Transition;
import org.springframework.statemachine.state.State;
import org.springframework.stereotype.Component;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.entity.Order;

import java.util.Optional;
import java.util.UUID;


@Component
public class OrderStateMachineInterceptor extends StateMachineInterceptorAdapter<OrderState, OrderEvent> {

    private final OrderRepository orderRepository;

    public OrderStateMachineInterceptor(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void preStateChange(State<OrderState, OrderEvent> state,
                           Message<OrderEvent> message,
                           Transition<OrderState, OrderEvent> transition,
                           StateMachine<OrderState, OrderEvent> stateMachine,
                           StateMachine<OrderState, OrderEvent> rootStateMachine) {
        
        if(message != null && message.getHeaders().containsKey(OrderService.ORDER_ID_HEADER)) {
            UUID orderId = (UUID) message.getHeaders().get(OrderService.ORDER_ID_HEADER);
            Optional<Order> order = orderRepository.findById(orderId);

            if(order.isPresent()) {
                Order existingOrder = order.get();
                existingOrder.setStatus(state.getId());
                orderRepository.save(existingOrder);
            }
            
        }

    }

}
