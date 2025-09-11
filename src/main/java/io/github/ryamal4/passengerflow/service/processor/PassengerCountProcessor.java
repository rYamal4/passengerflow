package io.github.ryamal4.passengerflow.service.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class PassengerCountProcessor {
    private static final Logger log = LoggerFactory.getLogger(PassengerCountProcessor.class);

    private final PassengerCountCalculationService calculationService;

    public PassengerCountProcessor(PassengerCountCalculationService calculationService) {
        this.calculationService = calculationService;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void processYesterdayData() {
        // we assume that every bus stops working somewhere before 00:00
        // and starts working somewhere after 00:00 each day
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Processing passenger counts for date: {}", yesterday);
        calculationService.calculateCurrentPassengersForDate(yesterday);
        log.info("Completed processing passenger counts for date: {}", yesterday);
    }
}