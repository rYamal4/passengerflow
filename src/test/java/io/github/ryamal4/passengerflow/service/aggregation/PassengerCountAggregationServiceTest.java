package io.github.ryamal4.passengerflow.service.aggregation;

import io.github.ryamal4.passengerflow.repository.IPassengerCountAggregationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;

import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class PassengerCountAggregationServiceTest {

    @Mock
    private IPassengerCountAggregationRepository aggregationRepository;

    @InjectMocks
    private PassengerCountAggregationService aggregationService;

    @Test
    void performDailyAggregation_CallsRepositoryMethodsInCorrectOrder() {
        DayOfWeek targetDayOfWeek = DayOfWeek.MONDAY;

        aggregationService.performDailyAggregation(targetDayOfWeek);

        InOrder inOrder = inOrder(aggregationRepository);
        inOrder.verify(aggregationRepository).deleteByDayOfWeek(1);
        inOrder.verify(aggregationRepository).insertAggregatedData(1);
    }
}