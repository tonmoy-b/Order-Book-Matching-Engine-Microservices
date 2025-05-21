package com.tbhatta.matchingengine.controller;

import com.tbhatta.matchingengine.service.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
//@RequestMapping("/check")
public class MatchingEngineController {
    @Autowired private KafkaConsumer kafkaConsumer;

    @GetMapping("/hello")
    public String sayHello() {
        //kafkaConsumer.receiveOrderItemEvent();
        return "Hello, World!";
    }
}
