package io.github.ryamal4.passengerflow.service.aggregation;

import io.github.ryamal4.passengerflow.repository.IPassengerCountAggregationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;

@Service
@RequiredArgsConstructor
@Slf4j
public class PassengerCountAggregationService implements IPassengerCountAggregationService {
    private final IPassengerCountAggregationRepository aggregationRepository;

    @Transactional
    public void performAggregation(DayOfWeek targetDayOfWeek) {
        log.info("Starting daily aggregation for day of week: {}", targetDayOfWeek);
        int dayOfWeekValue = targetDayOfWeek.getValue();

        aggregationRepository.deleteByDayOfWeek(dayOfWeekValue);
        log.info("Deleted old aggregations for day of week: {}", targetDayOfWeek);

        int insertedCount = aggregationRepository.insertAggregatedData(dayOfWeekValue);
        log.info("Inserted {} aggregation records for day of week: {}", insertedCount, targetDayOfWeek);
    }
}