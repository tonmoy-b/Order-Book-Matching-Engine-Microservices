package com.tbhatta.matchingengine.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;

@Document(collection = "fir01" )
public class TransactionItemModel {

    @Id
    private String TransactionID;
    private String mainClientID;
    private String counterPartyID;
    private String mainClientOrderId;
    private String counterPartOrderId;
    private String mainClientOrderType;
    private BigDecimal mainClientTransactionAmount;
    private BigDecimal spreadAmount;

    public TransactionItemModel() {
    }

    public TransactionItemModel(String transactionID, String mainClientID, String counterPartyID, String mainclientOrderId, String counterPartOrderId, String mainClientOrderType, BigDecimal mainClientTransactionAmount, BigDecimal spreadAmount) {
        TransactionID = transactionID;
        this.mainClientID = mainClientID;
        this.counterPartyID = counterPartyID;
        this.mainClientOrderId = mainclientOrderId;
        this.counterPartOrderId = counterPartOrderId;
        this.mainClientOrderType = mainClientOrderType;
        this.mainClientTransactionAmount = mainClientTransactionAmount;
        this.spreadAmount = spreadAmount;
    }

    @Override
    public String toString() {
        return "TransactionItemModel{" +
                "TransactionID=" + TransactionID +
                ", mainClientID='" + mainClientID + '\'' +
                ", counterPartyID='" + counterPartyID + '\'' +
                ", mainClientOrderId='" + mainClientOrderId + '\'' +
                ", counterPartOrderId='" + counterPartOrderId + '\'' +
                ", mainClientOrderType='" + mainClientOrderType + '\'' +
                ", mainClientTransactionAmount=" + mainClientTransactionAmount +
                ", spreadAmount=" + spreadAmount +
                '}';
    }

    public String getTransactionID() {
        return TransactionID;
    }

    public void setTransactionID(String transactionID) {
        TransactionID = transactionID;
    }

    public String getMainClientID() {
        return mainClientID;
    }

    public void setMainClientID(String mainClientID) {
        this.mainClientID = mainClientID;
    }

    public String getCounterPartyID() {
        return counterPartyID;
    }

    public void setCounterPartyID(String counterPartyID) {
        this.counterPartyID = counterPartyID;
    }

    public String getMainclientOrderId() {
        return mainClientOrderId;
    }

    public void setMainclientOrderId(String mainClientOrderId) {
        this.mainClientOrderId = mainClientOrderId;
    }

    public String getCounterPartOrderId() {
        return counterPartOrderId;
    }

    public void setCounterPartOrderId(String counterPartOrderId) {
        this.counterPartOrderId = counterPartOrderId;
    }

    public String getMainClientOrderType() {
        return mainClientOrderType;
    }

    public void setMainClientOrderType(String mainClientOrderType) {
        this.mainClientOrderType = mainClientOrderType;
    }

    public BigDecimal getMainClientTransactionAmount() {
        return mainClientTransactionAmount;
    }

    public void setMainClientTransactionAmount(BigDecimal mainClientTransactionAmount) {
        this.mainClientTransactionAmount = mainClientTransactionAmount;
    }

    public BigDecimal getSpreadAmount() {
        return spreadAmount;
    }

    public void setSpreadAmount(BigDecimal spreadAmount) {
        this.spreadAmount = spreadAmount;
    }
}
