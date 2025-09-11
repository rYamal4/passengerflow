package io.github.ryamal4.passengerflow.service.weather;

import io.github.ryamal4.passengerflow.service.IWeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.TimeZone;

@Slf4j
@Service
public class WeatherService implements IWeatherService {
    @Value("${open-meteo.api.url}")
    private String apiUrl;
    private final RestTemplate restTemplate;

    public WeatherService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean isRaining(LocalDateTime dateTime, Double latitude, Double longitude, TimeZone timeZone) {
        var date = dateTime.toLocalDate().toString();
        var url = UriComponentsBuilder.fromUriString(apiUrl)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("hourly", "weather_code")
                .queryParam("timezone", timeZone.getDisplayName())
                .queryParam("start_date", date)
                .queryParam("end_date", date)
                .build()
                .toUriString();

        try {
            WeatherResponseDto response = restTemplate.getForObject(url, WeatherResponseDto.class);
            if (response != null && response.getHourly() != null) {
                var hour = dateTime.getHour();
                return response.getHourly().getWeatherCode().get(hour) > 60;
            }
        } catch (RestClientException e) {
            log.error("Failed to get weather data for time = {} lat = {}, lon = {}, returning false by default: {}",
                    dateTime, latitude, longitude, e.getMessage());
        }

        return false;
    }
}
