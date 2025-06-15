package com.tbhatta.matchingengine.order_records.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.tbhatta.matchingengine.order_records.entity.OrderRecord;
import com.tbhatta.matchingengine.order_records.repository.OrderRecordsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderRecordService {

    @Autowired private OrderRecordsRepository orderRecordsRepository;

    public OrderRecordService() {
    }

    public void saveP(String name, String detail) {
        orderRecordsRepository.insert(new OrderRecord(name, detail));
        //MongoOperations mongoOps = new MongoTemplate(MongoClients.create("mongodb://localhost:27017"), "databaseeee");
        ///mongoOps.insert(new OrderRecord(name, detail));
        //MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");

    }
}
