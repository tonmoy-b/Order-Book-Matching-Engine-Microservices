package com.tbhatta.orderfront.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Entity
public class OrderItem {

    private static final Logger log = LoggerFactory.getLogger(OrderItem.class);
    @Id
    @GeneratedValue(strategy =  GenerationType.AUTO)
    private UUID orderId;

    @NotNull
    private String clientId;

    @NotNull
    private String asset;

    @NotNull
    private LocalDateTime orderTime;

    @NotNull
    private String orderType;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private BigInteger volume;

    private static final String dtPattern = "yyyy-MM-dd HH:mm:ss";//"yyyy-MM-dd HH:mm:ss.SSS";

    public LocalDateTime placeServiceDatetime(String strDateTime) throws Exception {
        try {
            DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern(dtPattern);
            return LocalDateTime.parse(strDateTime, dateTimeFormat);
        } catch (Exception e) {
            log.info("Exception in parsing Datetime.",e);
            throw e;
        }
    }

    public OrderItem(UUID orderId, String clientId, String asset, LocalDateTime orderTime, String orderType, BigDecimal amount, BigInteger volume) {
        this.orderId = orderId;
        this.clientId = clientId;
        this.asset = asset;
        this.orderTime = orderTime;
        this.orderType = orderType;
        this.amount = amount;
        this.volume = volume;
    }

    public OrderItem() {
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

    public void setOrderTime(String strDateTime) throws Exception{
        this.orderTime = placeServiceDatetime( strDateTime);
    }

    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
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

    public BigInteger getVolume() {
        return volume;
    }

    public void setVolume(BigInteger volume) {
        this.volume = volume;
    }
}
