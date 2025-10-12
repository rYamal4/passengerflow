package io.github.ryamal4.passengerflow.service.weather;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.TimeZone;

@Slf4j
@Service
public class WeatherService implements IWeatherService {
    public static final int RAIN_THRESHOLD_CODE = 60;

    private final RestClient restClient;
    @Value("${open-meteo.api.url}")
    private String apiUrl;

    public WeatherService(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @Cacheable(value = "weather", keyGenerator = "weatherCacheKeyGenerator")
    public boolean isRaining(LocalDateTime dateTime, Double latitude, Double longitude, TimeZone timeZone) {
        var date = dateTime.toLocalDate().toString();
        var url = UriComponentsBuilder.fromUriString(apiUrl)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("hourly", "weather_code")
                .queryParam("timezone", timeZone.getID())
                .queryParam("start_date", date)
                .queryParam("end_date", date)
                .build()
                .toUriString();

        try {
            WeatherResponseDto response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(WeatherResponseDto.class);
            if (response != null && response.getHourly() != null) {
                var hour = dateTime.getHour();
                return response.getHourly().getWeatherCode().get(hour) > RAIN_THRESHOLD_CODE;
            }
        } catch (RestClientException e) {
            log.error("Failed to get weather data for time = {} lat = {}, lon = {}, returning false by default: {}",
                    dateTime, latitude, longitude, e.getMessage());
        }

        return false;
    }
}
