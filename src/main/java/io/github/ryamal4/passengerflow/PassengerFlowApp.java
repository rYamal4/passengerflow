package io.github.ryamal4.passengerflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PassengerFlowApp {
    public static void main(String[] args) {
        SpringApplication.run(PassengerFlowApp.class, args);
    }
}
