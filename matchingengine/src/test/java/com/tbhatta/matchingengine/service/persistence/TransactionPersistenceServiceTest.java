package com.tbhatta.matchingengine.service.persistence;

import com.tbhatta.matchingengine.model.OrderItemModel;
import com.tbhatta.matchingengine.model.TransactionItemModel;
import com.tbhatta.matchingengine.order_records.repository.TransactionItemRepository;
import com.tbhatta.matchingengine.service.matching.MatchResult;
import com.tbhatta.matchingengine.service.matching.FillType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static com.tbhatta.matchingengine.service.matching.OrderBookTestFixtures.CLIENT_A;
import static com.tbhatta.matchingengine.service.matching.OrderBookTestFixtures.ask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static com.tbhatta.matchingengine.service.matching.OrderBookTestFixtures.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionPersistenceServiceTest {
    @Mock
    TransactionItemRepository transactionItemRepository;
    @InjectMocks
    TransactionPersistenceService transactionPersistenceService;


    @Test
    @DisplayName("One MatchResult - exactly two inserts (one insertion per ordering party)")
    void oneFill_producesTwoInserts() {
        MatchResult fill = exactFill(ask(CLIENT_A, "100.00", 10), bid(CLIENT_B, "100.00", 10), 10);
        transactionPersistenceService.persistAll(List.of(fill));
        verify(transactionItemRepository, times(2)).insert(any(TransactionItemModel.class));
    }

    @Test
    @DisplayName("Three MatchResults --> exactly six inserts (two per ordering party")
    void threeFills_producesSixInserts() {
        List<MatchResult> fills = List.of(
                exactFill(ask(CLIENT_A, "100.00", 5), bid(CLIENT_B, "100.00", 5), 5),
                exactFill(ask(CLIENT_A, "100.00", 5), bid(CLIENT_C, "100.00", 5), 5),
                exactFill(ask(CLIENT_A, "100.00", 5), bid(CLIENT_B, "101.00", 5), 5)
        );
        transactionPersistenceService.persistAll(fills);
        verify(transactionItemRepository, times(6)).insert(any(TransactionItemModel.class));
    }

    @Test
    @DisplayName("Empty results list - zero inserts")
    void noFills_zeroInserts() {
        transactionPersistenceService.persistAll(List.of());
        verifyNoInteractions(transactionItemRepository);
    }

    @Test
    @DisplayName("Transaction records have correct client IDs on both sides")
    void transactionRecords_correctClientIds() {
        var incoming = ask(CLIENT_A, "100.00", 10);
        var matched = bid(CLIENT_B, "102.00", 10);
        MatchResult fill = exactFill(incoming, matched, 10); //perfect match
        transactionPersistenceService.persistAll(List.of(fill));
        ArgumentCaptor<TransactionItemModel> captor = ArgumentCaptor.forClass(TransactionItemModel.class);
        verify(transactionItemRepository, times(2)).insert(captor.capture());
        List<TransactionItemModel> saved = captor.getAllValues();
        // insert incomingOrder as mainClient
        TransactionItemModel incomingTransaction = saved.get(0);
        assertThat(incomingTransaction.getMainClientID()).isEqualTo(CLIENT_A);
        assertThat(incomingTransaction.getCounterPartyID()).isEqualTo(CLIENT_B);
        assertThat(incomingTransaction.getMainClientOrderType()).isEqualTo("ask");
        // insert matchedOrder as mainClient (mirrored now)
        TransactionItemModel counterpartyTransaction = saved.get(1);
        assertThat(counterpartyTransaction.getMainClientID()).isEqualTo(CLIENT_B);
        assertThat(counterpartyTransaction.getCounterPartyID()).isEqualTo(CLIENT_A);
        assertThat(counterpartyTransaction.getMainClientOrderType()).isEqualTo("bid");
    }

    @Test
    @DisplayName("Transaction records have correct fill volumes")
    void transactionRecords_correctVolume() {
        MatchResult fill = exactFill(ask(CLIENT_A, "100.00", 7), bid(CLIENT_B, "100.00", 7), 7);
        transactionPersistenceService.persistAll(List.of(fill));
        ArgumentCaptor<TransactionItemModel> captor = ArgumentCaptor.forClass(TransactionItemModel.class);
        verify(transactionItemRepository, times(2)).insert(captor.capture());
        captor.getAllValues().forEach(transaction ->
                assertThat(transaction.getTransactionVolume()).isEqualTo(BigInteger.valueOf(7))
        );
    }

    @Test
    @DisplayName("Spread is negated correctly for counterparty records")
    void spreadIsNegatedForCounterparty() {
        // bid=102, ask=100
        // spread from ask perspective = 102 - 100 = +2
        // spread from bid perspective = 100 - 102 = -2
        var incoming = ask(CLIENT_A, "100.00", 5);
        var matched = bid(CLIENT_B, "102.00", 5);
        MatchResult fill = exactFill(incoming, matched, 5);
        // Manually set spread to simulate AskMatchingStrategy processing
        MatchResult fillWithSpread = new MatchResult(
                incoming, matched,
                BigInteger.valueOf(5),
                new BigDecimal("2.00"),  // bid.amount - ask.amount
                FillType.EXACT_FULL
        );
        transactionPersistenceService.persistAll(List.of(fillWithSpread));
        ArgumentCaptor<TransactionItemModel> captor = ArgumentCaptor.forClass(TransactionItemModel.class);
        verify(transactionItemRepository, times(2)).insert(captor.capture());
        List<TransactionItemModel> saved = captor.getAllValues();
        assertThat(saved.get(0).getSpreadAmount()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(saved.get(1).getSpreadAmount()).isEqualByComparingTo(new BigDecimal("-2.00"));
    }

    @Test
    @DisplayName("Repository failure on one result does not abort remaining inserts")
    void repositoryFailure_doesNotAbortOtherInserts() {
        var fill1 = exactFill(ask(CLIENT_A, "100.00", 5), bid(CLIENT_B, "100.00", 5), 5);
        var fill2 = exactFill(ask(CLIENT_A, "100.00", 5), bid(CLIENT_C, "100.00", 5), 5);
        // Scenario order of calls:
        // Call 1 (fill1): Throws Exception. persist() quits at this point.
        // Call 2 (fill2): Returns model.
        // Call 3 (fill2): Returns model.
        doThrow(new RuntimeException("Mongo unavailable"))
                .doReturn(new TransactionItemModel())
                .when(transactionItemRepository).insert(any(TransactionItemModel.class));
        transactionPersistenceService.persistAll(List.of(fill1, fill2));
        // fill1 attempted 1 insert and failed. persistAll must continue to fill2
        // fill2 attempted 2 inserts and succeeded.
        // Total = 3 invocations.
        verify(transactionItemRepository, times(3)).insert(any(TransactionItemModel.class));
    }

    // helper functions
    private static MatchResult exactFill(OrderItemModel incoming, OrderItemModel matched, int volume) {
        return new MatchResult(
                incoming, matched,
                BigInteger.valueOf(volume),
                BigDecimal.ZERO,
                FillType.EXACT_FULL
        );
    }
}