package io.github.ryamal4.passengerflow.service.aggregation;

import io.github.ryamal4.passengerflow.repository.IPassengerCountAggregationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

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
}