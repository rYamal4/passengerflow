package io.github.ryamal4.passengerflow.service.prediction;

import io.github.ryamal4.passengerflow.dto.OccupancyPredictionDTO;
import io.github.ryamal4.passengerflow.model.PassengerCountAggregation;
import io.github.ryamal4.passengerflow.repository.IPassengerCountAggregationRepository;
import io.github.ryamal4.passengerflow.service.weather.IWeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

@Service
@RequiredArgsConstructor
@Slf4j
public class OccupancyPredictionService implements IOccupancyPredictionService {
    private static final TimeZone MOSCOW_TIMEZONE = TimeZone.getTimeZone("Europe/Moscow");
    private static final ZoneId MOSCOW_ZONE_ID = ZoneId.of("Europe/Moscow");
    private static final double RAIN_OCCUPANCY_INCREASE = 20.0;

    private final IPassengerCountAggregationRepository aggregationRepository;
    private final IWeatherService weatherService;

    @Override
    public Optional<OccupancyPredictionDTO> getPrediction(String routeName, String stopName, LocalTime time, boolean useWeather) {
        var now = LocalDateTime.now(MOSCOW_ZONE_ID);
        var dayOfWeek = now.getDayOfWeek().getValue();
        var targetDateTime = now.with(time);

        int hour = time.getHour();
        int minute = roundToNearestFiveMinutes(time.getMinute());

        var aggregationOpt = aggregationRepository.findByRouteAndStopAndTime(
                routeName, stopName, dayOfWeek, hour, minute
        );

        return aggregationOpt.map(aggregation -> {
            var baseOccupancy = aggregation.getAverageOccupancyPercentage();
            var adjustedOccupancy = useWeather
                    ? adjustOccupancyForWeather(baseOccupancy, targetDateTime, aggregation)
                    : baseOccupancy;
            return new OccupancyPredictionDTO(stopName, time, adjustedOccupancy);
        });
    }

    @Override
    public List<OccupancyPredictionDTO> getTodayPredictions(String routeName, boolean useWeather) {
        var now = LocalDateTime.now(MOSCOW_ZONE_ID);
        var dayOfWeek = now.getDayOfWeek().getValue();

        var aggregations = aggregationRepository.findByRouteAndDayOfWeek(routeName, dayOfWeek);

        return aggregations.stream()
                .map(aggregation -> {
                    var time = LocalTime.of(aggregation.getHour(), aggregation.getMinute());
                    var targetDateTime = now.with(time);
                    var baseOccupancy = aggregation.getAverageOccupancyPercentage();
                    var adjustedOccupancy = useWeather
                            ? adjustOccupancyForWeather(baseOccupancy, targetDateTime, aggregation)
                            : baseOccupancy;
                    return new OccupancyPredictionDTO(aggregation.getStop().getName(), time, adjustedOccupancy);
                })
                .toList();
    }

    private double adjustOccupancyForWeather(double baseOccupancy, LocalDateTime targetDateTime, PassengerCountAggregation aggregation) {
        var stop = aggregation.getStop();
        boolean isRaining = weatherService.isRaining(targetDateTime, stop.getLat(), stop.getLon(), MOSCOW_TIMEZONE);
        if (isRaining) {
            log.debug("Rain detected for stop {} at {}, adding {}% to occupancy", stop.getName(), targetDateTime, RAIN_OCCUPANCY_INCREASE);
            return baseOccupancy + RAIN_OCCUPANCY_INCREASE;
        }
        return baseOccupancy;
    }

    private int roundToNearestFiveMinutes(int minute) {
        return (minute / 5) * 5;
    }
}
