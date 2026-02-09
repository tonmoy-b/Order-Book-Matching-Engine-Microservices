package com.tbhatta.orderbook_microservices_apigateway.exception;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Map;

@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> map = super.getErrorAttributes(request, options);
        Throwable error = getError(request);

        // Add the actual exception message to the response body
        map.put("message", error.getMessage());
        map.put("exception", error.getClass().getSimpleName());

        // For your specific 500 error, this will likely reveal
        // "JwtValidationException: The iss claim is not valid"
        return map;
    }
}