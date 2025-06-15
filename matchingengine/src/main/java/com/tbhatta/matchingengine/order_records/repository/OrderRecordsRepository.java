package com.tbhatta.matchingengine.order_records.repository;

import com.tbhatta.matchingengine.order_records.entity.OrderRecord;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRecordsRepository extends MongoRepository<OrderRecord, String> {

    public OrderRecord findByName(String name);
}
