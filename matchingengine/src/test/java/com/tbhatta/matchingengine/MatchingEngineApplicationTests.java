package com.tbhatta.matchingengine;

import com.tbhatta.matchingengine.order_records.repository.TransactionItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
               // "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
               // "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
})
class MatchingEngineApplicationTests {

    @MockitoBean
    private TransactionItemRepository transactionItemRepository;

    @Test
    void contextLoads() {
    }

}
