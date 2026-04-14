package com.tbhatta.orderfront;

import java.time.format.DateTimeFormatter;

// amulti-use constants collected here for uniformity
public class OrderfrontConstants {
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    public static final String KAFKA_TOPIC_ORDER_ITEM = "orderitem";

    public OrderfrontConstants() {
    }


}
