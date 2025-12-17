package org.example.sporty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Sporty Live Event Tracker microservice.
 *
 * This application tracks live sports events and periodically fetches updates
 * from external APIs, publishing them to Kafka for downstream processing.
 */
@SpringBootApplication
@EnableScheduling
public class SportyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportyApplication.class, args);
    }
}

