package io.github.ryamal4.passengerflow.service.weather;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WeatherCacheKeyGeneratorTest {
    private static final TimeZone TIMEZONE = TimeZone.getTimeZone("Europe/Moscow");
    private WeatherCacheKeyGenerator keyGenerator;

    @BeforeEach
    void setUp() {
        keyGenerator = new WeatherCacheKeyGenerator();
    }

    @Test
    void testGenerateKeyRoundsCoordinatesToGrid() {
        var dateTime = LocalDateTime.of(2025, 9, 4, 10, 0);
        var lat1 = 52.521234;
        var lon1 = 13.411789;
        var lat2 = 52.524567;
        var lon2 = 13.414123;

        var key1 = keyGenerator.generate(null, null, dateTime, lat1, lon1, TIMEZONE);
        var key2 = keyGenerator.generate(null, null, dateTime, lat2, lon2, TIMEZONE);

        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void testGenerateKeyRoundsTimeToFiveMinutes() {
        var dateTime1 = LocalDateTime.of(2025, 9, 4, 10, 3, 45);
        var dateTime2 = LocalDateTime.of(2025, 9, 4, 10, 7, 12);
        var latitude = 52.52;
        var longitude = 13.41;

        var key1 = keyGenerator.generate(null, null, dateTime1, latitude, longitude, TIMEZONE);
        var key2 = keyGenerator.generate(null, null, dateTime2, latitude, longitude, TIMEZONE);

        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void testGenerateKeyDifferentDaysProduceDifferentKeys() {
        var dateTime1 = LocalDateTime.of(2025, 9, 4, 10, 0);
        var dateTime2 = LocalDateTime.of(2025, 9, 5, 10, 0);
        var latitude = 52.52;
        var longitude = 13.41;

        var key1 = keyGenerator.generate(null, null, dateTime1, latitude, longitude, TIMEZONE);
        var key2 = keyGenerator.generate(null, null, dateTime2, latitude, longitude, TIMEZONE);

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void testGenerateKeyDifferentTimezonesProduceDifferentKeys() {
        var dateTime = LocalDateTime.of(2025, 9, 4, 10, 0);
        var latitude = 52.52;
        var longitude = 13.41;
        var timezone1 = TimeZone.getTimeZone("Europe/Moscow");
        var timezone2 = TimeZone.getTimeZone("America/New_York");

        var key1 = keyGenerator.generate(null, null, dateTime, latitude, longitude, timezone1);
        var key2 = keyGenerator.generate(null, null, dateTime, latitude, longitude, timezone2);

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void testGenerateKeyDifferentCoordinatesProduceDifferentKeys() {
        var dateTime = LocalDateTime.of(2025, 9, 4, 10, 0);
        var lat1 = 52.52;
        var lon1 = 13.41;
        var lat2 = 52.53;
        var lon2 = 13.42;

        var key1 = keyGenerator.generate(null, null, dateTime, lat1, lon1, TIMEZONE);
        var key2 = keyGenerator.generate(null, null, dateTime, lat2, lon2, TIMEZONE);

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void testGenerateKeyWhenTooManyParams() {
        var dateTime = LocalDateTime.of(2025, 9, 4, 10, 0);
        var lat1 = 52.52;
        var lon1 = 13.41;

        assertThrows(IllegalArgumentException.class, () -> {
            keyGenerator.generate(null, null, dateTime, lat1, lon1, TIMEZONE, null);
        });
    }
}
