package com.tbhatta.matchingengine.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.math.BigInteger;

@Document(collection = "fir01" )
public class TransactionItemModel {

    @Id
    private String TransactionID;
    private String mainClientID;
    private String counterPartyID;
    private String mainClientOrderId;
    private String counterPartyOrderId;
    private String mainClientOrderType;
    private BigDecimal mainClientTransactionAmount;
    private BigDecimal spreadAmount;
    private BigInteger transactionVolume;

    public BigInteger getTransactionVolume() {
        return transactionVolume;
    }

    public void setTransactionVolume(BigInteger transactionVolume) {
        this.transactionVolume = transactionVolume;
    }

    public String getMainClientOrderId() {
        return mainClientOrderId;
    }

    public void setMainClientOrderId(String mainClientOrderId) {
        this.mainClientOrderId = mainClientOrderId;
    }

    public TransactionItemModel() {
    }

    public TransactionItemModel(String transactionID, String mainClientID, String counterPartyID, String mainclientOrderId, String counterPartyOrderId, String mainClientOrderType, BigDecimal mainClientTransactionAmount, BigDecimal spreadAmount, BigInteger transactionVolume) {
        TransactionID = transactionID;
        this.mainClientID = mainClientID;
        this.counterPartyID = counterPartyID;
        this.mainClientOrderId = mainclientOrderId;
        this.counterPartyOrderId = counterPartyOrderId;
        this.mainClientOrderType = mainClientOrderType;
        this.mainClientTransactionAmount = mainClientTransactionAmount;
        this.spreadAmount = spreadAmount;
        this.transactionVolume = transactionVolume;
    }

    @Override
    public String toString() {
        return "TransactionItemModel{" +
                "TransactionID=" + TransactionID +
                ", mainClientID='" + mainClientID + '\'' +
                ", counterPartyID='" + counterPartyID + '\'' +
                ", mainClientOrderId='" + mainClientOrderId + '\'' +
                ", counterPartOrderId='" + counterPartyOrderId + '\'' +
                ", mainClientOrderType='" + mainClientOrderType + '\'' +
                ", mainClientTransactionAmount=" + mainClientTransactionAmount +
                ", spreadAmount=" + spreadAmount +
                ", transactionVolume=" + transactionVolume +
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

    public String getCounterPartyOrderId() {
        return counterPartyOrderId;
    }

    public void setCounterPartyOrderId(String counterPartyOrderId) {
        this.counterPartyOrderId = counterPartyOrderId;
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
