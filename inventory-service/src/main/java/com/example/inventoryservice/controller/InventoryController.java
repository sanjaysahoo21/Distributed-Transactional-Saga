package com.example.inventoryservice.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;

import java.util.UUID;


@RestController
public class InventoryController {

    @PostMapping("/inventory/reserve")
    public ResponseEntity<?> reserveInventory(@RequestParam UUID orderId, @RequestParam(required = false) Integer quantity){

        if(orderId == null){
            return ResponseEntity.badRequest().body("Order ID is required");
        }

        if(quantity == null){
            return ResponseEntity.badRequest().body("Quantity is required");
        }
        
        if(quantity > 100) {
            System.err.println("Inventory reservation failed for Order: " + orderId + " Out of Stock");
            return ResponseEntity.badRequest().body("Inventory reservation failed: Out of Stock");
        }

        System.out.println("Inventory reserved for Order: " + orderId + ", Quantity: " + quantity);
        return ResponseEntity.ok("Inventory reserved successfully");

    }

    @PostMapping("/inventory/release")
    public ResponseEntity<?> releaseInventory(@RequestParam UUID orderId){
        if(orderId == null){
            return ResponseEntity.badRequest().body("Order ID is required");
        }
        System.out.println("Releasing inventory for Order: " + orderId);
        return ResponseEntity.ok("Inventory released successfully");
    }
}

