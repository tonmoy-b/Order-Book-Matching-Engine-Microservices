package com.tbhatta.orderfront.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.tbhatta.orderfront.dto.CreateOrderItemRequest;
import com.tbhatta.orderfront.dto.OrderItemResponse;
import com.tbhatta.orderfront.entity.OrderItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class OrderItemMapperTest {

    @Test
    void toResponse_mapsAllFieldsCorrectly() {
        UUID id = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        OrderItem entity = new OrderItem(
                id, "CLIENT-001", "AAPL",
                LocalDateTime.of(2025, 5, 20, 10, 30, 0),
                "BUY", new BigDecimal("1500.50"), new BigInteger("100")
        );
        OrderItemResponse response = OrderItemMapper.toResponse(entity);
        assertThat(response.orderId()).isEqualTo(id.toString());
        assertThat(response.clientId()).isEqualTo("CLIENT-001");
        assertThat(response.asset()).isEqualTo("AAPL");
        assertThat(response.orderTime()).isEqualTo("2025-05-20 10:30:00");
        assertThat(response.orderType()).isEqualTo("BUY");
        assertThat(response.amount()).isEqualTo("1500.5");
        assertThat(response.volume()).isEqualTo("100");
    }

    @Test
    void toResponse_stripsTrailingZerosFromAmount() {
        OrderItem entity = new OrderItem(
                UUID.randomUUID(), "CLIENT-002", "TSLA",
                LocalDateTime.of(2025, 1, 1, 0, 0, 0),
                "SELL", new BigDecimal("999.00"), new BigInteger("200")
        );
        OrderItemResponse response = OrderItemMapper.toResponse(entity);
        assertThat(response.amount()).isEqualTo("999");
    }

    @Test
    void toEntity_mapsFieldsAndUsesProvidedTimestamp() {
        CreateOrderItemRequest request = new CreateOrderItemRequest(
                "CLIENT-003", "GOOG", "buy", "5000.75", "25"
        );
        LocalDateTime fixedTime = LocalDateTime.of(2025, 6, 15, 14, 0, 0);
        OrderItem entity = OrderItemMapper.toEntity(request, fixedTime);
        assertThat(entity.getOrderId()).isNull();
        assertThat(entity.getClientId()).isEqualTo("CLIENT-003");
        assertThat(entity.getAsset()).isEqualTo("GOOG");
        assertThat(entity.getOrderType()).isEqualTo("BUY"); // uppercased
        assertThat(entity.getOrderTime()).isEqualTo(fixedTime);
        assertThat(entity.getAmount()).isEqualByComparingTo(new BigDecimal("5000.75"));
        assertThat(entity.getVolume()).isEqualTo(new BigInteger("25"));
    }

    @Test
    void toEntity_uppercasesOrderType() {
        CreateOrderItemRequest request = new CreateOrderItemRequest(
                "CLIENT-004", "MSFT", "sell", "100.00", "10"
        );
        OrderItem entity = OrderItemMapper.toEntity(request, LocalDateTime.now());
        assertThat(entity.getOrderType()).isEqualTo("SELL");
    }

    @Test
    void toEntity_invalidAmountThrowsNumberFormatException() {
        CreateOrderItemRequest request = new CreateOrderItemRequest(
                "CLIENT-005", "AMZN", "BUY", "not-a-number", "10"
        );
        assertThatThrownBy(() -> OrderItemMapper.toEntity(request, LocalDateTime.now()))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void toEntity_invalidVolumeThrowsNumberFormatException() {
        CreateOrderItemRequest request = new CreateOrderItemRequest(
                "CLIENT-006", "NFLX", "SELL", "100.00", "abc"
        );
        assertThatThrownBy(() -> OrderItemMapper.toEntity(request, LocalDateTime.now()))
                .isInstanceOf(NumberFormatException.class);
    }


}