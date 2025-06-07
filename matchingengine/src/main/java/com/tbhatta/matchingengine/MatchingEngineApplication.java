package com.tbhatta.matchingengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MatchingEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(MatchingEngineApplication.class, args);
	}

}
