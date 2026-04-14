package com.tbhatta.orderfront.controller;

import com.tbhatta.orderfront.dto.CreateOrderItemRequest;
import com.tbhatta.orderfront.dto.OrderItemDTO;
import com.tbhatta.orderfront.dto.OrderItemResponse;
import com.tbhatta.orderfront.dto.OrderItemResponseDTO;
import com.tbhatta.orderfront.service.OrderItemService;
import jakarta.persistence.PostRemove;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/orders")
public class OrderItemController {

    private final OrderItemService orderItemService;

    public OrderItemController(OrderItemService orderItemService) {
        this.orderItemService = orderItemService;
    }

    @GetMapping
    public ResponseEntity<Page<OrderItemResponse>> getAllOrderItems(@PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(orderItemService.getAllOrders(pageable));
    }

    @PostMapping
    public ResponseEntity<OrderItemResponse> createOrderItem(
            @Valid @RequestBody CreateOrderItemRequest request) {
        OrderItemResponse created = orderItemService.createOrderItem(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)   // Http code 201
                .body(created);
    }


}
