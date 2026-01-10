package io.github.ryamal4.passengerflow.service.weather;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.TimeZone;

@Slf4j
@Component
public class WeatherDataFetcher {
    private static final double COORDINATE_GRID_PRECISION = 0.01;

    private final RestClient restClient;
    @Value("${open-meteo.api.url}")
    private String apiUrl;

    public WeatherDataFetcher(RestClient restClient) {
        this.restClient = restClient;
    }

    @Cacheable(value = "weather", keyGenerator = "weatherCacheKeyGenerator")
    public WeatherResponseDto fetchWeatherData(LocalDateTime dateTime, Double latitude, Double longitude, TimeZone timeZone) {
        var date = dateTime.toLocalDate().toString();
        var roundedLat = roundToGrid(latitude);
        var roundedLon = roundToGrid(longitude);
        var url = UriComponentsBuilder.fromUriString(apiUrl)
                .queryParam("latitude", roundedLat)
                .queryParam("longitude", roundedLon)
                .queryParam("hourly", "weather_code")
                .queryParam("timezone", timeZone.getID())
                .queryParam("start_date", date)
                .queryParam("end_date", date)
                .build()
                .toUriString();

        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(WeatherResponseDto.class);
        } catch (RestClientException e) {
            log.error("Failed to get weather data for date = {}, lat = {}, lon = {}: {}",
                    date, latitude, longitude, e.getMessage());
        }

        return null;
    }

    private double roundToGrid(double coordinate) {
        return Math.round(coordinate / COORDINATE_GRID_PRECISION) * COORDINATE_GRID_PRECISION;
    }
}
