package com.tbhatta.orderfront.controller;

import com.tbhatta.orderfront.dto.OrderItemDTO;
import com.tbhatta.orderfront.service.OrderItemService;
import jakarta.persistence.PostRemove;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderItemController {

    private OrderItemService orderItemService;

    public OrderItemController(OrderItemService orderItemService) {
        this.orderItemService = orderItemService;
    }

    @GetMapping
    public ResponseEntity<List<OrderItemDTO>> getAllOrderItems() {
        return ResponseEntity.ok()
                        .body(orderItemService.getAllOrders());
    }

    @PostMapping
    public ResponseEntity<OrderItemDTO> createOrderItem (@Valid @RequestBody OrderItemDTO orderItemDTO) {
        OrderItemDTO createdOrderItemDTO = orderItemService.createOrderItem(orderItemDTO);
        return ResponseEntity.ok()
                .body(createdOrderItemDTO);
    }


}
