package io.github.ryamal4.passengerflow.service.stop;

import io.github.ryamal4.passengerflow.model.Stop;
import io.github.ryamal4.passengerflow.repository.IStopsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StopsServiceTest {
    @Mock
    private IStopsRepository stopsRepository;
    @Mock
    private Stop testStop;

    @InjectMocks
    private StopsService stopsService;

    @Test
    void testGetNearbyStopsReturns5Stops() {
        when(stopsRepository.findNearbyStops(anyDouble(), anyDouble(), eq(5)))
                .thenReturn(List.of(testStop, testStop, testStop, testStop, testStop));

        var stops = stopsService.getNearbyStops(0, 0);

        assertThat(stops).hasSize(5);
    }
}
