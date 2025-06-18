package com.tbhatta.matchingengine.model;

import com.tbhatta.protos.me.OrderItemEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class OrderItemModel {
    private static final String dtPattern = "yyyy-MM-dd HH:mm:ss";
    private static final Logger log = LoggerFactory.getLogger(OrderItemModel.class);
    private UUID orderId;
    private String clientId;
    private String asset;
    private LocalDateTime orderTime;
    private String orderType;
    private BigDecimal amount;
    private BigInteger volume;

    public OrderItemModel(UUID orderId, String clientId, String asset, LocalDateTime orderTime, String orderType, BigDecimal amount, BigInteger volume) {
        this.orderId = orderId;
        this.clientId = clientId;
        this.asset = asset;
        this.orderTime = orderTime;
        this.orderType = orderType;
        this.amount = amount;
        this.volume = volume;
    }

    public OrderItemModel() {
    }

    public OrderItemModel(OrderItemEvent orderItemEvent) {
        this.orderId = UUID.fromString(orderItemEvent.getOrderId());
        this.clientId = orderItemEvent.getClientId();
        this.asset = orderItemEvent.getAsset();
        this.orderTime = stringToLocalDateTime(orderItemEvent.getOrderTime());
        this.orderType = orderItemEvent.getOrderType();
        this.amount = new BigDecimal(orderItemEvent.getAmount());
        this.volume = new BigInteger(orderItemEvent.getVolume());

    }

    @Override
    public String toString() {
        return "OrderItemModel{" +
                "orderId=" + orderId +
                ", clientId='" + clientId + '\'' +
                ", asset='" + asset + '\'' +
                ", orderTime=" + orderTime +
                ", orderType='" + orderType + '\'' +
                ", amount=" + amount +
                ", volume=" + volume +
                '}';
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }

    public void setOrderTime(String strOrderTime) {
        try {
            DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern(dtPattern);
            this.orderTime =  LocalDateTime.parse(strOrderTime, dateTimeFormat);
        } catch (Exception e) {
            log.info("MEService, Exception in parsing Datetime.",e);
            throw e;
        }
    }

    private LocalDateTime stringToLocalDateTime(String s) {
        try {
            DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern(dtPattern);
            return  LocalDateTime.parse(s, dateTimeFormat);
        } catch (Exception e) {
            log.info("MEService, Exception in parsing Datetime.",e);
            throw e;
        }
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setAmount(String strAmount) {
        this.amount = new BigDecimal(strAmount);
    }

    public BigInteger getVolume() {
        return volume;
    }

    public void setVolume(BigInteger volume) {
        this.volume = volume;
    }

    public void setVolume(String strVolume) {
        this.volume = new BigInteger(strVolume);
    }

    public static OrderItemModel makeCopy (OrderItemModel original) {
        var copy = new OrderItemModel();
        copy.orderId = original.getOrderId();
        copy.clientId = original.getClientId();
        copy.asset = original.getAsset();
        copy.orderTime = original.getOrderTime();
        copy.orderType = original.getOrderType();
        copy.amount = original.getAmount();
        copy.volume = original.getVolume();
        return copy;
    }
}
