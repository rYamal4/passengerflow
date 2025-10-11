package io.github.ryamal4.passengerflow.repository;

import io.github.ryamal4.passengerflow.AbstractTestContainerTest;
import io.github.ryamal4.passengerflow.model.Route;
import io.github.ryamal4.passengerflow.model.Stop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StopsRepositoryTest extends AbstractTestContainerTest {
    private static final String STOP_1 = "stop 1";
    private static final String STOP_2 = "stop 2";
    private static final String STOP_3 = "stop 3";
    private static final String STOP_4 = "stop 4";
    public static final double LAT_60_1699 = 60.1699;
    public static final double LON_24_9342 = 24.9342;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IStopsRepository stopsRepository;

    private Route testRoute;

    @BeforeEach
    void setUp() {
        testRoute = new Route();
        testRoute.setName("TestRoute");
        entityManager.persistAndFlush(testRoute);
    }

    @Test
    void testGetNearbyStops_ReturnsSortedByDistance() {
        Stop nearStop = createStop(STOP_1, 60.1695, 24.9354, testRoute);
        Stop middleStop = createStop(STOP_2, 60.1750, 24.9400, testRoute);
        Stop farStop = createStop(STOP_3, 60.1800, 24.9450, testRoute);

        entityManager.persistAndFlush(nearStop);
        entityManager.persistAndFlush(middleStop);
        entityManager.persistAndFlush(farStop);

        List<Stop> result = stopsRepository.findNearbyStops(LAT_60_1699, LON_24_9342, 3);

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(nearStop);
        assertThat(result.get(1)).isEqualTo(middleStop);
        assertThat(result.get(2)).isEqualTo(farStop);
    }

    @Test
    void testGetNearbyStops_RespectsLimit() {
        Stop stop1 = createStop(STOP_1, 60.1695, 24.9354, testRoute);
        Stop stop2 = createStop(STOP_2, 60.1696, 24.9355, testRoute);
        Stop stop3 = createStop(STOP_3, 60.1697, 24.9356, testRoute);
        Stop stop4 = createStop(STOP_4, 60.1698, 24.9357, testRoute);

        entityManager.persistAndFlush(stop1);
        entityManager.persistAndFlush(stop2);
        entityManager.persistAndFlush(stop3);
        entityManager.persistAndFlush(stop4);

        List<Stop> result = stopsRepository.findNearbyStops(LAT_60_1699, LON_24_9342, 2);

        assertThat(result).hasSize(2);
    }

    @Test
    void testGetNearbyStops_EmptyDatabase() {
        List<Stop> result = stopsRepository.findNearbyStops(LAT_60_1699, LON_24_9342, 5);

        assertThat(result).isEmpty();
    }

    @Test
    void testGetNearbyStops_WithZeroCount() {
        Stop stop = createStop(STOP_1, 60.1695, 24.9354, testRoute);
        entityManager.persistAndFlush(stop);

        List<Stop> result = stopsRepository.findNearbyStops(LAT_60_1699, LON_24_9342, 0);

        assertThat(result).isEmpty();
    }

    @Test
    void testGetNearbyStops_BoundaryCoordinates() {
        Stop nearNorthPoleStop = createStop(STOP_1, 89.9999, 0.0, testRoute);
        Stop slightlyFurtherStop = createStop(STOP_2, 89.9950, 0.0, testRoute);

        entityManager.persistAndFlush(nearNorthPoleStop);
        entityManager.persistAndFlush(slightlyFurtherStop);

        List<Stop> result = stopsRepository.findNearbyStops(90.0, 0.0, 2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(nearNorthPoleStop);
        assertThat(result.get(1)).isEqualTo(slightlyFurtherStop);
    }

    @Test
    void testGetNearbyStops_CalculatesDistanceCorrectly() {
        Stop nearestStop = createStop(STOP_1, LAT_60_1699, LON_24_9342, testRoute);
        Stop furtherStop = createStop(STOP_2, 60.1686, 24.9324, testRoute);

        entityManager.persistAndFlush(nearestStop);
        entityManager.persistAndFlush(furtherStop);

        List<Stop> result = stopsRepository.findNearbyStops(LAT_60_1699, LON_24_9342, 2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(nearestStop);
        assertThat(result.get(1)).isEqualTo(furtherStop);
    }

    @Test
    void testGetNearbyStops_ExcludesStopsOutsideBoundingBox() {
        Stop nearStop = createStop(STOP_1, 60.1695, 24.9354, testRoute);
        Stop farStop = createStop(STOP_2, 61.0000, 25.0000, testRoute);

        entityManager.persistAndFlush(nearStop);
        entityManager.persistAndFlush(farStop);

        List<Stop> result = stopsRepository.findNearbyStops(LAT_60_1699, LON_24_9342, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(nearStop);
    }

    private Stop createStop(String name, double lat, double lon, Route route) {
        Stop stop = new Stop();
        stop.setName(name);
        stop.setLat(lat);
        stop.setLon(lon);
        stop.setRoute(route);
        return stop;
    }
}