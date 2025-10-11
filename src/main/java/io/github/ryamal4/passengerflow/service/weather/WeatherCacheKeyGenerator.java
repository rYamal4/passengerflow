package io.github.ryamal4.passengerflow.service.weather;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.TimeZone;

@Component("weatherCacheKeyGenerator")
public class WeatherCacheKeyGenerator implements KeyGenerator {
    private static final double COORDINATE_GRID_PRECISION = 0.01;
    private static final int TIME_ROUNDING_MINUTES = 5;

    @Override
    public String generate(Object target, Method method, Object... params) {
        if (params.length != 4) {
            throw new IllegalArgumentException("Expected 4 parameters for weather cache key generation");
        }

        var dateTime = (LocalDateTime) params[0];
        var latitude = (Double) params[1];
        var longitude = (Double) params[2];
        var timeZone = (TimeZone) params[3];

        var roundedDateTime = roundToNearestFiveMinutes(dateTime);
        var roundedLatitude = roundToGrid(latitude);
        var roundedLongitude = roundToGrid(longitude);

        return String.format("%s_%s_%s_%s",
                roundedDateTime,
                roundedLatitude,
                roundedLongitude,
                timeZone.getID()
        );
    }

    private double roundToGrid(double coordinate) {
        return Math.round(coordinate / COORDINATE_GRID_PRECISION) * COORDINATE_GRID_PRECISION;
    }

    private LocalDateTime roundToNearestFiveMinutes(LocalDateTime dateTime) {
        var roundedMinute = (int) (Math.round(dateTime.getMinute() / (double) TIME_ROUNDING_MINUTES) * TIME_ROUNDING_MINUTES);
        return dateTime
                .withMinute(roundedMinute)
                .withSecond(0)
                .withNano(0);
    }
}
