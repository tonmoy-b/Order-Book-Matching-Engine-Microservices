package com.tbhatta.matchingengine.service.persistence;

import com.tbhatta.matchingengine.order_records.repository.TransactionItemRepository;
import com.tbhatta.matchingengine.model.OrderItemModel;
import com.tbhatta.matchingengine.model.TransactionItemModel;
import com.tbhatta.matchingengine.service.matching.MatchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionPersistenceService {
    private static final Logger log = LoggerFactory.getLogger(TransactionPersistenceService.class);
    private final TransactionItemRepository transactionItemRepository;

    public TransactionPersistenceService(TransactionItemRepository transactionItemRepository) {
        this.transactionItemRepository = transactionItemRepository;
    }

    public void persistAll(List<MatchResult> results) {
        for (MatchResult result : results) {
            try {
                persist(result);
            } catch (Exception e) {
                // Log and continue — one bad record should not abort the rest
                log.error("Failed to persist MatchResult [incoming={}, matched={}, volume={}]: {}",
                        result.getIncomingOrder().getOrderId(),
                        result.getMatchedOrder().getOrderId(),
                        result.getFillVolume(),
                        e.getMessage(), e);
            }
        }
    }

    private void persist(MatchResult result) {
        OrderItemModel incoming = result.getIncomingOrder();
        OrderItemModel counterpart = result.getMatchedOrder();
        TransactionItemModel incomingTx = buildTransaction(incoming, counterpart, result);
        TransactionItemModel counterpartTx = buildTransaction(counterpart, incoming, result.mirrored());
        transactionItemRepository.insert(incomingTx);
        log.debug("Persisted incoming tx {} for order {}", incomingTx.getTransactionID(), incoming.getOrderId());
        transactionItemRepository.insert(counterpartTx);
        log.debug("Persisted counterpart tx {} for order {}", counterpartTx.getTransactionID(), counterpart.getOrderId());
    }

    private TransactionItemModel buildTransaction(
            OrderItemModel mainClient,
            OrderItemModel counterParty,
            MatchResult result
    ) {
        return TransactionItemModel.builder()
                .mainClientId(mainClient.getClientId())
                .counterPartyId(counterParty.getClientId())
                .mainClientOrderId(mainClient.getOrderId().toString())
                .counterPartyOrderId(counterParty.getOrderId().toString())
                .mainClientOrderType(mainClient.getOrderType())
                .mainClientTransactionAmount(mainClient.getAmount())
                .spreadAmount(result.getSpread())
                .transactionVolume(result.getFillVolume())
                .build();
    }
}
