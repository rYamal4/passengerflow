package io.github.ryamal4.passengerflow.service.prediction;

import io.github.ryamal4.passengerflow.model.PassengerCountAggregation;
import io.github.ryamal4.passengerflow.model.Route;
import io.github.ryamal4.passengerflow.model.Stop;
import io.github.ryamal4.passengerflow.repository.IPassengerCountAggregationRepository;
import io.github.ryamal4.passengerflow.service.weather.IWeatherService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OccupancyPredictionServiceTest {

    @Mock
    private IPassengerCountAggregationRepository aggregationRepository;

    @Mock
    private IWeatherService weatherService;

    @InjectMocks
    private OccupancyPredictionService predictionService;

    @Test
    void testGetPredictionReturnsDataWithoutRain() {
        var route = new Route(1L, "7A", List.of(), List.of());
        var stop = new Stop(1L, "Central Station", 55.7558, 37.6173, route, List.of());
        var aggregation = new PassengerCountAggregation(1L, stop, 1, 15, 0, 45.0);

        when(aggregationRepository.findByRouteAndStopAndTime(
                eq("7A"), eq("Central Station"), anyInt(), eq(15), eq(0)
        )).thenReturn(Optional.of(aggregation));

        when(weatherService.isRaining(any(), anyDouble(), anyDouble(), any())).thenReturn(false);

        var result = predictionService.getPrediction("7A", "Central Station", LocalTime.of(15, 0));

        assertThat(result).isPresent();
        assertThat(result.get().getStopName()).isEqualTo("Central Station");
        assertThat(result.get().getTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(result.get().getOccupancyPercentage()).isEqualTo(45.0);
    }

    @Test
    void testGetPredictionAddsRainBonus() {
        var route = new Route(1L, "7A", List.of(), List.of());
        var stop = new Stop(1L, "Central Station", 55.7558, 37.6173, route, List.of());
        var aggregation = new PassengerCountAggregation(1L, stop, 1, 15, 0, 45.0);

        when(aggregationRepository.findByRouteAndStopAndTime(
                eq("7A"), eq("Central Station"), anyInt(), eq(15), eq(0)
        )).thenReturn(Optional.of(aggregation));

        when(weatherService.isRaining(any(), anyDouble(), anyDouble(), any())).thenReturn(true);

        var result = predictionService.getPrediction("7A", "Central Station", LocalTime.of(15, 0));

        assertThat(result).isPresent();
        assertThat(result.get().getOccupancyPercentage()).isEqualTo(65.0);
    }

    @Test
    void testGetPredictionAllowsOccupancyOver100() {
        var route = new Route(1L, "7A", List.of(), List.of());
        var stop = new Stop(1L, "Central Station", 55.7558, 37.6173, route, List.of());
        var aggregation = new PassengerCountAggregation(1L, stop, 1, 15, 0, 90.0);

        when(aggregationRepository.findByRouteAndStopAndTime(
                eq("7A"), eq("Central Station"), anyInt(), eq(15), eq(0)
        )).thenReturn(Optional.of(aggregation));

        when(weatherService.isRaining(any(), anyDouble(), anyDouble(), any())).thenReturn(true);

        var result = predictionService.getPrediction("7A", "Central Station", LocalTime.of(15, 0));

        assertThat(result).isPresent();
        assertThat(result.get().getOccupancyPercentage()).isEqualTo(110.0);
    }

    @Test
    void testGetPredictionReturnsEmptyWhenNoAggregationData() {
        when(aggregationRepository.findByRouteAndStopAndTime(
                anyString(), anyString(), anyInt(), anyInt(), anyInt()
        )).thenReturn(Optional.empty());

        var result = predictionService.getPrediction("7A", "Central Station", LocalTime.of(15, 0));

        assertThat(result).isEmpty();
    }

    @Test
    void testGetPredictionRoundsMinutesToNearestFive() {
        var route = new Route(1L, "7A", List.of(), List.of());
        var stop = new Stop(1L, "Central Station", 55.7558, 37.6173, route, List.of());
        var aggregation = new PassengerCountAggregation(1L, stop, 1, 15, 0, 45.0);

        when(aggregationRepository.findByRouteAndStopAndTime(
                eq("7A"), eq("Central Station"), anyInt(), eq(15), eq(0)
        )).thenReturn(Optional.of(aggregation));

        when(weatherService.isRaining(any(), anyDouble(), anyDouble(), any())).thenReturn(false);

        var result = predictionService.getPrediction("7A", "Central Station", LocalTime.of(15, 3));

        assertThat(result).isPresent();
    }

    @Test
    void testGetTodayPredictionsReturnsAllStopsForRoute() {
        var route = new Route(1L, "7A", List.of(), List.of());
        var stop1 = new Stop(1L, "Central Station", 55.7558, 37.6173, route, List.of());
        var stop2 = new Stop(2L, "Downtown", 55.7558, 37.6173, route, List.of());
        var aggregation1 = new PassengerCountAggregation(1L, stop1, 1, 8, 0, 45.0);
        var aggregation2 = new PassengerCountAggregation(2L, stop2, 1, 9, 0, 60.0);

        when(aggregationRepository.findByRouteAndDayOfWeek(eq("7A"), anyInt()))
                .thenReturn(List.of(aggregation1, aggregation2));

        when(weatherService.isRaining(any(), anyDouble(), anyDouble(), any())).thenReturn(false);

        var result = predictionService.getTodayPredictions("7A");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStopName()).isEqualTo("Central Station");
        assertThat(result.get(0).getTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(result.get(1).getStopName()).isEqualTo("Downtown");
        assertThat(result.get(1).getTime()).isEqualTo(LocalTime.of(9, 0));
    }
}
