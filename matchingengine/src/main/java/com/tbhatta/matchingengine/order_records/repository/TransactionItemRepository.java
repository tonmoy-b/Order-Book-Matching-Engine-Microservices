package com.tbhatta.matchingengine.order_records.repository;

import com.tbhatta.matchingengine.model.TransactionItemModel;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface TransactionItemRepository extends MongoRepository<TransactionItemModel, String> {
}
