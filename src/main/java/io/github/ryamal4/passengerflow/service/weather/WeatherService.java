package io.github.ryamal4.passengerflow.service.weather;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.TimeZone;

@Service
@RequiredArgsConstructor
public class WeatherService implements IWeatherService {
    public static final int RAIN_THRESHOLD_CODE = 60;

    private final WeatherDataFetcher weatherDataFetcher;

    @Override
    public boolean isRaining(LocalDateTime dateTime, Double latitude, Double longitude, TimeZone timeZone) {
        var response = weatherDataFetcher.fetchWeatherData(dateTime, latitude, longitude, timeZone);
        if (response != null && response.getHourly() != null) {
            var hour = dateTime.getHour();
            return response.getHourly().getWeatherCode().get(hour) > RAIN_THRESHOLD_CODE;
        }
        return false;
    }
}
