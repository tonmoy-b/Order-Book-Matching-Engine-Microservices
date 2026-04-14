package com.tbhatta.orderfront.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tbhatta.orderfront.dto.CreateOrderItemRequest;
import com.tbhatta.orderfront.dto.OrderItemResponse;
import com.tbhatta.orderfront.service.OrderItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;

import static org.mockito.Mockito.when;

@WebMvcTest(OrderItemController.class)
public class OrderItemControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderItemService orderItemService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final OrderItemResponse SAMPLE_RESPONSE = new OrderItemResponse(
            "d4e5f6a7-b8c9-0d1e-2f3a-4b5c6d7e8f90",
            "CLIENT-001", "AAPL", "2025-05-20 10:30:00",
            "BUY", "1500.5", "100"
    );

    @Test
    void getAll_returnsPagedResults() throws Exception {
        Page<OrderItemResponse> page = new PageImpl<>(List.of(SAMPLE_RESPONSE));
        when(orderItemService.getAllOrders(any(Pageable.class))).thenReturn(page);
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].clientId", is("CLIENT-001")))
                .andExpect(jsonPath("$.content[0].asset", is("AAPL")));
    }

    @Test
    void getAll_emptyPage() throws Exception {
        when(orderItemService.getAllOrders(any(Pageable.class)))
                .thenReturn(Page.empty());
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        CreateOrderItemRequest request = new CreateOrderItemRequest(
                "CLIENT-001", "AAPL", "BUY", "1500.50", "100"
        );
        when(orderItemService.createOrderItem(any(CreateOrderItemRequest.class)))
                .thenReturn(SAMPLE_RESPONSE);
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId", notNullValue()))
                .andExpect(jsonPath("$.clientId", is("CLIENT-001")));
    }

    @Test
    void create_missingClientId_returns400() throws Exception {
        // clientId is null → @NotBlank should trigger
        String body = """
                {"asset":"AAPL","orderType":"BUY","amount":"100.00","volume":"10"}
                """;
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.clientId").exists());
    }

    @Test
    void create_invalidOrderType_returns400() throws Exception {
        String body = """
                {"clientId":"C1","asset":"AAPL","orderType":"INVALID","amount":"100.00","volume":"10"}
                """;
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.orderType").exists());
    }

    @Test
    void create_negativeAmount_returns400() throws Exception {
        String body = """
                {"clientId":"C1","asset":"AAPL","orderType":"BUY","amount":"-50","volume":"10"}
                """;
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.amount").exists());
    }
}
