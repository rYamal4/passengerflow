package io.github.ryamal4.passengerflow.service.aggregation;

import io.github.ryamal4.passengerflow.repository.IPassengerCountAggregationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PassengerCountAggregationServiceTest {

    @Mock
    private IPassengerCountAggregationRepository aggregationRepository;

    @InjectMocks
    private PassengerCountAggregationService aggregationService;

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void testPerformAggregationAllDaysOfWeek(DayOfWeek dayOfWeek) {
        int expectedValue = dayOfWeek.getValue();
        when(aggregationRepository.insertAggregatedData(expectedValue)).thenReturn(1);

        aggregationService.performAggregation(dayOfWeek);

        var inOrder = inOrder(aggregationRepository);
        inOrder.verify(aggregationRepository).deleteByDayOfWeek(expectedValue);
        inOrder.verify(aggregationRepository).insertAggregatedData(expectedValue);
        inOrder.verifyNoMoreInteractions();
    }
}