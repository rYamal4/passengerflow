package io.github.ryamal4.passengerflow.service.weather;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private WeatherService weatherService;

    private static final String API_URL = "https://test-api.com/";
    private static final double LATITUDE = 52.52;
    private static final double LONGITUDE = 13.41;
    private static final TimeZone TIMEZONE = TimeZone.getTimeZone("Europe/Moscow");

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(restTemplate);
        ReflectionTestUtils.setField(weatherService, "apiUrl", API_URL);
    }

    @Test
    void testIsRaining_ReturnsTrueForRainyWeatherCode() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 9, 4, 10, 0);

        WeatherResponseDto mockResponse = createMockWeatherResponse(
                Arrays.asList(3, 0, 1, 1, 1, 3, 3, 3, 2, 0, 61, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3)
        );

        when(restTemplate.getForObject(anyString(), eq(WeatherResponseDto.class)))
                .thenReturn(mockResponse);

        boolean result = weatherService.isRaining(dateTime, LATITUDE, LONGITUDE, TIMEZONE);

        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "0, false",
            "45, false",
            "60, false"
    })
    void testIsRaining_ReturnsFalseForNonRainyWeatherCodes(int weatherCode, boolean expectedResult) {
        LocalDateTime dateTime = LocalDateTime.of(2025, 9, 4, 10, 0);

        WeatherResponseDto mockResponse = createMockWeatherResponse(
                Arrays.asList(3, 0, 1, 1, 1, 3, 3, 3, 2, 0, weatherCode, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3)
        );

        when(restTemplate.getForObject(anyString(), eq(WeatherResponseDto.class)))
                .thenReturn(mockResponse);

        boolean result = weatherService.isRaining(dateTime, LATITUDE, LONGITUDE, TIMEZONE);

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void testIsRaining_ReturnsFalseWhenResponseIsNull() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 9, 4, 10, 0);

        when(restTemplate.getForObject(anyString(), eq(WeatherResponseDto.class)))
                .thenReturn(null);

        boolean result = weatherService.isRaining(dateTime, LATITUDE, LONGITUDE, TIMEZONE);

        assertThat(result).isFalse();
    }

    @Test
    void testIsRaining_ReturnsFalseWhenHourlyDataIsNull() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 9, 4, 10, 0);

        WeatherResponseDto mockResponse = new WeatherResponseDto();
        mockResponse.setHourly(null);

        when(restTemplate.getForObject(anyString(), eq(WeatherResponseDto.class)))
                .thenReturn(mockResponse);

        boolean result = weatherService.isRaining(dateTime, LATITUDE, LONGITUDE, TIMEZONE);

        assertThat(result).isFalse();
    }

    @Test
    void testIsRaining_ReturnsFalseWhenRestClientExceptionThrown() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 9, 4, 10, 0);

        when(restTemplate.getForObject(anyString(), eq(WeatherResponseDto.class)))
                .thenThrow(new RestClientException("API unavailable"));

        boolean result = weatherService.isRaining(dateTime, LATITUDE, LONGITUDE, TIMEZONE);

        assertThat(result).isFalse();
    }

    @Test
    void testIsRaining_HandlesHourAtMidnight() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 9, 4, 0, 0);

        WeatherResponseDto mockResponse = createMockWeatherResponse(
                Arrays.asList(80, 0, 1, 1, 1, 3, 3, 3, 2, 0, 45, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3)
        );

        when(restTemplate.getForObject(anyString(), eq(WeatherResponseDto.class)))
                .thenReturn(mockResponse);

        boolean result = weatherService.isRaining(dateTime, LATITUDE, LONGITUDE, TIMEZONE);

        assertThat(result).isTrue();
    }

    @Test
    void testIsRaining_HandlesHourAt23() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 9, 4, 23, 0);

        WeatherResponseDto mockResponse = createMockWeatherResponse(
                Arrays.asList(3, 0, 1, 1, 1, 3, 3, 3, 2, 0, 45, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 85)
        );

        when(restTemplate.getForObject(anyString(), eq(WeatherResponseDto.class)))
                .thenReturn(mockResponse);

        boolean result = weatherService.isRaining(dateTime, LATITUDE, LONGITUDE, TIMEZONE);

        assertThat(result).isTrue();
    }

    private WeatherResponseDto createMockWeatherResponse(List<Integer> weatherCodes) {
        HourlyDataDto hourlyData = new HourlyDataDto();
        hourlyData.setWeatherCode(weatherCodes);

        WeatherResponseDto response = new WeatherResponseDto();
        response.setHourly(hourlyData);

        return response;
    }
}