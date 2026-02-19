package com.example.orderservice.state;

public enum OrderEvent {
    CREATE_ORDER,
    PAYMENT_SUCCESS,
    PAYMENT_FAILURE,
    INVENTORY_SUCCESS,
    INVENTORY_FAILURE,
    CANCEL_ORDER
}
