package com.tbhatta.matchingengine.order_records.service;

import com.tbhatta.matchingengine.model.TransactionItemModel;
import com.tbhatta.matchingengine.order_records.repository.TransactionItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionRecordService {

    private final TransactionItemRepository transactionItemRepository;

    public TransactionRecordService(TransactionItemRepository transactionItemRepository) {
        this.transactionItemRepository = transactionItemRepository;
    }

    public void saveTransaction_(TransactionItemModel model) {
        transactionItemRepository.insert(model);
    }

    public void setTransaction_(String ClientId, String Price) {
        var tran = new TransactionItemModel();
        tran.setMainClientID(ClientId);
        tran.setMainClientTransactionAmount(new BigDecimal(Price));
        transactionItemRepository.insert(tran);
    }
}
