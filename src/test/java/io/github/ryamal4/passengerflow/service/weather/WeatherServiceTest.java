package io.github.ryamal4.passengerflow.service.weather;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {
    private static final String API_URL = "https://test-api.com/";
    private static final double LATITUDE = 52.52;
    private static final double LONGITUDE = 13.41;
    private static final TimeZone TIMEZONE = TimeZone.getTimeZone("Europe/Moscow");
    private static final LocalDateTime TEST_DATE_TIME = LocalDateTime.of(2025, 9, 4, 10, 0);
    private static final int RAINY_WEATHER_CODE = 61;

    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;
    @Mock
    private RestClient.RequestHeadersSpec<?> requestHeadersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;
    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(restClient);
        ReflectionTestUtils.setField(weatherService, "apiUrl", API_URL);
    }

    @Test
    void testIsRainingReturnsTrueForRainyWeatherCode() {
        var mockResponse = createMockWeatherResponseForHour(TEST_DATE_TIME.getHour(), RAINY_WEATHER_CODE);
        setupRestClientMocks();
        doReturn(mockResponse).when(responseSpec).body(WeatherResponseDto.class);

        var result = weatherService.isRaining(TEST_DATE_TIME, LATITUDE, LONGITUDE, TIMEZONE);

        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"0", "45", "60"})
    void testIsRainingReturnsFalseForNonRainyWeatherCodes(int weatherCode) {
        var mockResponse = createMockWeatherResponseForHour(TEST_DATE_TIME.getHour(), weatherCode);
        setupRestClientMocks();
        doReturn(mockResponse).when(responseSpec).body(WeatherResponseDto.class);

        var result = weatherService.isRaining(TEST_DATE_TIME, LATITUDE, LONGITUDE, TIMEZONE);

        assertThat(result).isFalse();
    }

    @Test
    void testIsRainingReturnsFalseWhenResponseIsNull() {
        setupRestClientMocks();
        doReturn(null).when(responseSpec).body(WeatherResponseDto.class);

        var result = weatherService.isRaining(TEST_DATE_TIME, LATITUDE, LONGITUDE, TIMEZONE);

        assertThat(result).isFalse();
    }

    @Test
    void testIsRainingReturnsFalseWhenRestClientExceptionThrown() {
        setupRestClientMocks();
        doThrow(new RestClientException("API unavailable")).when(responseSpec).body(WeatherResponseDto.class);

        var result = weatherService.isRaining(TEST_DATE_TIME, LATITUDE, LONGITUDE, TIMEZONE);

        assertThat(result).isFalse();
    }

    private void setupRestClientMocks() {
        doReturn(requestHeadersUriSpec).when(restClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString());
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
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