package io.github.ryamal4.passengerflow.service.aggregation;

import java.time.DayOfWeek;

public interface IPassengerCountAggregationService {
    void performDailyAggregation(DayOfWeek targetDayOfWeek);
}