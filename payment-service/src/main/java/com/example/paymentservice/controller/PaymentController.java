package com.example.paymentservice.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

@RestController
public class PaymentController {

    @PostMapping("/payment")
    public ResponseEntity<?> processPayment(@RequestParam UUID orderId, @RequestParam(required = false) Double amount){
        if(orderId == null){
            return ResponseEntity.badRequest().body("Order ID is required");
        }
        System.out.println("Processing payment for Order: " + orderId + ", Amount: " + amount);

        if(amount != null && amount > 1000){
            System.err.println("Payment Failed: Insufficient funds for Order: " + orderId);
            return ResponseEntity.badRequest().body("Payment Failed: Insufficient funds for Order: " + orderId);
        }
        return ResponseEntity.ok("Payment Processed Successfully for Order: " + orderId);
    }

    @PostMapping("/payment/cancel")
    public ResponseEntity<?> cancelPayment(@RequestParam UUID orderId){
        if(orderId == null){
            return ResponseEntity.badRequest().body("Order id is required");
        }
        System.out.println("Cancelling payment for Order: " + orderId);
        return ResponseEntity.ok("Payment Cancelled Successfully for Order: " + orderId);
    }

}
