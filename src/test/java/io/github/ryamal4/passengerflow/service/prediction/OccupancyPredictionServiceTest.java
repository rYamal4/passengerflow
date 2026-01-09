package io.github.ryamal4.passengerflow.service.prediction;

import io.github.ryamal4.passengerflow.model.PassengerCountAggregation;
import io.github.ryamal4.passengerflow.model.Route;
import io.github.ryamal4.passengerflow.model.Stop;
import io.github.ryamal4.passengerflow.repository.IPassengerCountAggregationRepository;
import io.github.ryamal4.passengerflow.service.weather.IWeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OccupancyPredictionServiceTest {

    private static final String ROUTE_NAME = "7A";
    private static final String STOP_NAME = "Central Station";

    @Mock
    private IPassengerCountAggregationRepository aggregationRepository;

    @Mock
    private IWeatherService weatherService;

    @InjectMocks
    private OccupancyPredictionService predictionService;

    private Route route;
    private Stop stop;

    @BeforeEach
    void setUp() {
        route = new Route(1L, ROUTE_NAME, List.of(), List.of());
        stop = new Stop(1L, STOP_NAME, 55.7558, 37.6173, route, List.of());
    }

    @Test
    void testGetPredictionReturnsDataWithoutRain() {
        var aggregation = createAggregation(stop, 15, 0, 45.0);

        when(aggregationRepository.findByRouteAndStopAndTime(
                eq(ROUTE_NAME), eq(STOP_NAME), anyInt(), eq(15), eq(0)
        )).thenReturn(Optional.of(aggregation));

        when(weatherService.isRaining(any(), anyDouble(), anyDouble(), any())).thenReturn(false);

        var result = predictionService.getPrediction(ROUTE_NAME, STOP_NAME, LocalTime.of(15, 0), true);

        assertThat(result).isPresent();
        assertThat(result.get().getStopName()).isEqualTo(STOP_NAME);
        assertThat(result.get().getTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(result.get().getOccupancyPercentage()).isEqualTo(45.0);
    }

    @Test
    void testGetPredictionAddsRainBonus() {
        var aggregation = createAggregation(stop, 15, 0, 45.0);

        when(aggregationRepository.findByRouteAndStopAndTime(
                eq(ROUTE_NAME), eq(STOP_NAME), anyInt(), eq(15), eq(0)
        )).thenReturn(Optional.of(aggregation));

        when(weatherService.isRaining(any(), anyDouble(), anyDouble(), any())).thenReturn(true);

        var result = predictionService.getPrediction(ROUTE_NAME, STOP_NAME, LocalTime.of(15, 0), true);

        assertThat(result).isPresent();
        assertThat(result.get().getOccupancyPercentage()).isEqualTo(65.0);
    }

    @Test
    void testGetPredictionAllowsOccupancyOver100() {
        var aggregation = createAggregation(stop, 15, 0, 90.0);

        when(aggregationRepository.findByRouteAndStopAndTime(
                eq(ROUTE_NAME), eq(STOP_NAME), anyInt(), eq(15), eq(0)
        )).thenReturn(Optional.of(aggregation));

        when(weatherService.isRaining(any(), anyDouble(), anyDouble(), any())).thenReturn(true);

        var result = predictionService.getPrediction(ROUTE_NAME, STOP_NAME, LocalTime.of(15, 0), true);

        assertThat(result).isPresent();
        assertThat(result.get().getOccupancyPercentage()).isEqualTo(110.0);
    }

    @Test
    void testGetPredictionReturnsEmptyWhenNoAggregationData() {
        when(aggregationRepository.findByRouteAndStopAndTime(
                anyString(), anyString(), anyInt(), anyInt(), anyInt()
        )).thenReturn(Optional.empty());

        var result = predictionService.getPrediction(ROUTE_NAME, STOP_NAME, LocalTime.of(15, 0), true);

        assertThat(result).isEmpty();
    }

    @Test
    void testGetPredictionRoundsMinutesToNearestFive() {
        var aggregation = createAggregation(stop, 15, 0, 45.0);

        when(aggregationRepository.findByRouteAndStopAndTime(
                eq(ROUTE_NAME), eq(STOP_NAME), anyInt(), eq(15), eq(0)
        )).thenReturn(Optional.of(aggregation));

        when(weatherService.isRaining(any(), anyDouble(), anyDouble(), any())).thenReturn(false);

        var result = predictionService.getPrediction(ROUTE_NAME, STOP_NAME, LocalTime.of(15, 3), true);

        assertThat(result).isPresent();
    }

    @Test
    void testGetPredictionWithoutWeatherSkipsWeatherService() {
        var aggregation = createAggregation(stop, 15, 0, 45.0);

        when(aggregationRepository.findByRouteAndStopAndTime(
                eq(ROUTE_NAME), eq(STOP_NAME), anyInt(), eq(15), eq(0)
        )).thenReturn(Optional.of(aggregation));

        var result = predictionService.getPrediction(ROUTE_NAME, STOP_NAME, LocalTime.of(15, 0), false);

        assertThat(result).isPresent();
        assertThat(result.get().getOccupancyPercentage()).isEqualTo(45.0);
        verifyNoInteractions(weatherService);
    }

    @Test
    void testGetTodayPredictionsReturnsAllStopsForRoute() {
        var stop2 = new Stop(2L, "Downtown", 55.7558, 37.6173, route, List.of());
        var aggregation1 = createAggregation(stop, 8, 0, 45.0);
        var aggregation2 = createAggregation(stop2, 9, 0, 60.0);

        when(aggregationRepository.findByRouteAndDayOfWeek(eq(ROUTE_NAME), anyInt()))
                .thenReturn(List.of(aggregation1, aggregation2));

        when(weatherService.isRaining(any(), anyDouble(), anyDouble(), any())).thenReturn(false);

        var result = predictionService.getTodayPredictions(ROUTE_NAME, true);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStopName()).isEqualTo(STOP_NAME);
        assertThat(result.get(0).getTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(result.get(1).getStopName()).isEqualTo("Downtown");
        assertThat(result.get(1).getTime()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    void testGetTodayPredictionsWithoutWeatherSkipsWeatherService() {
        var aggregation = createAggregation(stop, 8, 0, 45.0);

        when(aggregationRepository.findByRouteAndDayOfWeek(eq(ROUTE_NAME), anyInt()))
                .thenReturn(List.of(aggregation));

        var result = predictionService.getTodayPredictions(ROUTE_NAME, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOccupancyPercentage()).isEqualTo(45.0);
        verifyNoInteractions(weatherService);
    }

    private PassengerCountAggregation createAggregation(Stop stop, int hour, int minute, double occupancy) {
        return new PassengerCountAggregation(1L, stop, 1, hour, minute, occupancy);
    }
}
