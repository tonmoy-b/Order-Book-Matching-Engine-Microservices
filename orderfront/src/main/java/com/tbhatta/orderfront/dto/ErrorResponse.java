package com.tbhatta.orderfront.dto;

import java.time.Instant;
import java.util.Map;

// error responses for errors in OrderItem actions
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        Map<String, String> details

) {
    public static ErrorResponse of (int status, String error) {
        return new ErrorResponse(Instant.now(), status, error, Map.of());
    }

    public static ErrorResponse of (int status, String error, Map<String, String> details ) {
        return new ErrorResponse(Instant.now(), status, error, details);
    }

    public static ErrorResponse of (Instant timestamp, int status, String error, Map<String, String> details) {
        return new ErrorResponse(timestamp, status, error, details);
    }
}
