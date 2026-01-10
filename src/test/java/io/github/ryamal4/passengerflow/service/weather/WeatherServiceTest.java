package io.github.ryamal4.passengerflow.service.weather;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {
    private static final double LATITUDE = 52.52;
    private static final double LONGITUDE = 13.41;
    private static final TimeZone TIMEZONE = TimeZone.getTimeZone("Europe/Moscow");
    private static final LocalDateTime TEST_DATE_TIME = LocalDateTime.of(2025, 9, 4, 10, 0);
    private static final int RAINY_WEATHER_CODE = 61;

    @Mock
    private WeatherDataFetcher weatherDataFetcher;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(weatherDataFetcher);
    }

    @Test
    void testIsRainingReturnsTrueForRainyWeatherCode() {
        var mockResponse = createMockWeatherResponseForHour(TEST_DATE_TIME.getHour(), RAINY_WEATHER_CODE);
        when(weatherDataFetcher.fetchWeatherData(TEST_DATE_TIME, LATITUDE, LONGITUDE, TIMEZONE))
                .thenReturn(mockResponse);

        var result = weatherService.isRaining(TEST_DATE_TIME, LATITUDE, LONGITUDE, TIMEZONE);

        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"0", "45", "60"})
    void testIsRainingReturnsFalseForNonRainyWeatherCodes(int weatherCode) {
        var mockResponse = createMockWeatherResponseForHour(TEST_DATE_TIME.getHour(), weatherCode);
        when(weatherDataFetcher.fetchWeatherData(TEST_DATE_TIME, LATITUDE, LONGITUDE, TIMEZONE))
                .thenReturn(mockResponse);

        var result = weatherService.isRaining(TEST_DATE_TIME, LATITUDE, LONGITUDE, TIMEZONE);

        assertThat(result).isFalse();
    }

    @Test
    void testIsRainingReturnsFalseWhenResponseIsNull() {
        when(weatherDataFetcher.fetchWeatherData(TEST_DATE_TIME, LATITUDE, LONGITUDE, TIMEZONE))
                .thenReturn(null);

        var result = weatherService.isRaining(TEST_DATE_TIME, LATITUDE, LONGITUDE, TIMEZONE);

        assertThat(result).isFalse();
    }

    private WeatherResponseDto createMockWeatherResponseForHour(int hour, int weatherCode) {
        var weatherCodes = new ArrayList<>(
                Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        );
        weatherCodes.set(hour, weatherCode);

        var hourlyData = new HourlyDataDto();
        hourlyData.setWeatherCode(weatherCodes);

        var response = new WeatherResponseDto();
        response.setHourly(hourlyData);

        return response;
    }
}