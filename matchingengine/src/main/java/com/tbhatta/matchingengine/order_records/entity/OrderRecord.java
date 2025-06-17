package com.tbhatta.matchingengine.order_records.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "OrderRecord")
public class OrderRecord {

    @Id public String id;
    public String name;
    public String remark;

    public OrderRecord() {
    }

    public OrderRecord(String id, String name, String remark) {
        this.id = id;
        this.name = name;
        this.remark = remark;
    }

    public OrderRecord(String name, String remark) {
        this.name = name;
        this.remark = remark;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "OrderRecord [" +
                "id=" + id  +
                ", name=" + name  +
                ", remark=" + remark  +
                ']';
    }
}
