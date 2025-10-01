package io.github.ryamal4.passengerflow.scheduler;

import io.github.ryamal4.passengerflow.service.aggregation.IPassengerCountAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class PassengerCountAggregationJob {
    private final IPassengerCountAggregationService aggregationService;

    @Scheduled(cron = "0 0 4 * * *")
    public void performDailyAggregation() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            log.info("Starting daily passenger count aggregation job for day of week: {}", yesterday.getDayOfWeek());
            aggregationService.performDailyAggregation(yesterday.getDayOfWeek());
            log.info("Daily passenger count aggregation job completed successfully");
        } catch (Exception e) {
            log.error("Error during daily passenger count aggregation", e);
            throw e;
        }
    }
}