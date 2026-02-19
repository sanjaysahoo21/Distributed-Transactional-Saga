package com.example.orderservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import com.example.orderservice.service.SagaActions;
import com.example.orderservice.state.OrderState;
import com.example.orderservice.state.OrderEvent;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory
public class StateMachineConfig extends EnumStateMachineConfigurerAdapter<OrderState, OrderEvent> {

    private final SagaActions sagaActions;

    public StateMachineConfig(SagaActions sagaActions) {
        this.sagaActions = sagaActions;
    }

    @Override
    public void configure(StateMachineStateConfigurer<OrderState, OrderEvent> states) throws Exception {
        states.withStates()
                .initial(OrderState.ORDER_CREATED)
                // State entry actions - execute when entering the state
                .state(OrderState.PAYMENT_PENDING, sagaActions.processPaymentAction(), null)
                .state(OrderState.PAYMENT_COMPLETED, sagaActions.reserveInventoryAction(), null)
                .state(OrderState.INVENTORY_RESERVED)
                .end(OrderState.ORDER_COMPLETED)
                .end(OrderState.ORDER_FAILED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderState, OrderEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(OrderState.ORDER_CREATED)
                .target(OrderState.PAYMENT_PENDING)
                .event(OrderEvent.CREATE_ORDER)
                .and()
                .withExternal()
                .source(OrderState.PAYMENT_PENDING)
                .target(OrderState.PAYMENT_COMPLETED)
                .event(OrderEvent.PAYMENT_SUCCESS)
                .and()
                .withExternal()
                .source(OrderState.PAYMENT_PENDING)
                .target(OrderState.ORDER_FAILED)
                .event(OrderEvent.PAYMENT_FAILURE)
                .and()
                .withExternal()
                .source(OrderState.PAYMENT_COMPLETED)
                .target(OrderState.INVENTORY_RESERVED)
                .event(OrderEvent.INVENTORY_SUCCESS)
                .and()
                .withExternal()
                .source(OrderState.PAYMENT_COMPLETED)
                .target(OrderState.ORDER_FAILED)
                .event(OrderEvent.INVENTORY_FAILURE)
                .action(sagaActions.compensatePaymentAction())
                .and()
                .withExternal()
                .source(OrderState.INVENTORY_RESERVED)
                .target(OrderState.ORDER_COMPLETED)
                .event(OrderEvent.COMPLETE_ORDER);
    }
}
