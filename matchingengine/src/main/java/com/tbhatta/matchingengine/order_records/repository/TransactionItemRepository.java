package com.tbhatta.matchingengine.order_records.repository;

import com.tbhatta.matchingengine.model.TransactionItemModel;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface TransactionItemRepository extends MongoRepository<TransactionItemModel, String> {

    //TransactionItemModel findByTransactionItemModelMainClientID(String mainClientID);
    List<TransactionItemModel> getBymainClientID(String mainClientID);
    List<TransactionItemModel> getByTransactionID(String TransactionID);
    List<TransactionItemModel> getBymainClientOrderId(String mainClientOrderId);
}
