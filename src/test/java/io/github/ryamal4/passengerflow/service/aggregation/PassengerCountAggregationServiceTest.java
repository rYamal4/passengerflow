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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PassengerCountAggregationServiceTest {

    @Mock
    private IPassengerCountAggregationRepository aggregationRepository;

    @InjectMocks
    private PassengerCountAggregationService aggregationService;

    @Test
    void testPerformAggregationCallsRepositoryMethodsInCorrectOrder() {
        when(aggregationRepository.insertAggregatedData(1)).thenReturn(5);

        aggregationService.performAggregation(DayOfWeek.MONDAY);

        var inOrder = inOrder(aggregationRepository);
        inOrder.verify(aggregationRepository).deleteByDayOfWeek(1);
        inOrder.verify(aggregationRepository).insertAggregatedData(1);
    }

    @Test
    void testPerformAggregationUsesCorrectDayOfWeekValue() {
        when(aggregationRepository.insertAggregatedData(7)).thenReturn(10);

        aggregationService.performAggregation(DayOfWeek.SUNDAY);

        var inOrder = inOrder(aggregationRepository);
        inOrder.verify(aggregationRepository).deleteByDayOfWeek(7);
        inOrder.verify(aggregationRepository).insertAggregatedData(7);
    }

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void testPerformAggregationAllDaysOfWeek(DayOfWeek dayOfWeek) {
        int expectedValue = dayOfWeek.getValue();
        when(aggregationRepository.insertAggregatedData(expectedValue)).thenReturn(1);

        aggregationService.performAggregation(dayOfWeek);

        verify(aggregationRepository).deleteByDayOfWeek(expectedValue);
        verify(aggregationRepository).insertAggregatedData(expectedValue);
    }

    @Test
    void testPerformAggregationDeletesBeforeInsert() {
        when(aggregationRepository.insertAggregatedData(anyInt())).thenReturn(0);

        aggregationService.performAggregation(DayOfWeek.WEDNESDAY);

        var inOrder = inOrder(aggregationRepository);
        inOrder.verify(aggregationRepository).deleteByDayOfWeek(3);
        inOrder.verify(aggregationRepository).insertAggregatedData(3);
        inOrder.verifyNoMoreInteractions();
    }
}