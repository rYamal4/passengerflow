package io.github.ryamal4.passengerflow.persistence.repository;

import io.github.ryamal4.passengerflow.persistence.entities.RouteEntity;
import io.github.ryamal4.passengerflow.persistence.entities.StopEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class IStopsRepositoryTest {
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

    private RouteEntity testRoute;

    @BeforeEach
    void setUp() {
        testRoute = new RouteEntity();
        testRoute.setName("TestRoute");
        entityManager.persistAndFlush(testRoute);
    }

    @Test
    void testGetNearbyStops_ReturnsSortedByDistance() {
        StopEntity nearStop = createStop(STOP_1, 60.1695, 24.9354, testRoute);
        StopEntity farStop = createStop(STOP_2, 61.0000, 25.0000, testRoute);
        StopEntity middleStop = createStop(STOP_3, 60.5000, 24.9500, testRoute);

        entityManager.persistAndFlush(nearStop);
        entityManager.persistAndFlush(farStop);
        entityManager.persistAndFlush(middleStop);

        List<StopEntity> result = stopsRepository.getNearbyStops(LAT_60_1699, LON_24_9342, 3);

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(nearStop);
        assertThat(result.get(2)).isEqualTo(farStop);
    }

    @Test
    void testGetNearbyStops_RespectsLimit() {
        StopEntity stop1 = createStop(STOP_1, 60.1695, 24.9354, testRoute);
        StopEntity stop2 = createStop(STOP_2, 60.1696, 24.9355, testRoute);
        StopEntity stop3 = createStop(STOP_3, 60.1697, 24.9356, testRoute);
        StopEntity stop4 = createStop(STOP_4, 60.1698, 24.9357, testRoute);

        entityManager.persistAndFlush(stop1);
        entityManager.persistAndFlush(stop2);
        entityManager.persistAndFlush(stop3);
        entityManager.persistAndFlush(stop4);

        List<StopEntity> result = stopsRepository.getNearbyStops(LAT_60_1699, LON_24_9342, 2);

        assertThat(result).hasSize(2);
    }

    @Test
    void testGetNearbyStops_EmptyDatabase() {
        List<StopEntity> result = stopsRepository.getNearbyStops(LAT_60_1699, LON_24_9342, 5);

        assertThat(result).isEmpty();
    }

    @Test
    void testGetNearbyStops_WithZeroCount() {
        StopEntity stop = createStop(STOP_1, 60.1695, 24.9354, testRoute);
        entityManager.persistAndFlush(stop);

        List<StopEntity> result = stopsRepository.getNearbyStops(LAT_60_1699, LON_24_9342, 0);

        assertThat(result).isEmpty();
    }

    @Test
    void testGetNearbyStops_BoundaryCoordinates() {
        StopEntity northPoleStop = createStop(STOP_1, 89.9999, 0.0, testRoute);
        StopEntity equatorStop = createStop(STOP_2, 0.0, 0.0, testRoute);

        entityManager.persistAndFlush(northPoleStop);
        entityManager.persistAndFlush(equatorStop);

        List<StopEntity> result = stopsRepository.getNearbyStops(90.0, 0.0, 2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(northPoleStop);
    }

    @Test
    void testGetNearbyStops_CalculatesDistanceCorrectly() {
        StopEntity nearestStop = createStop(STOP_1, LAT_60_1699, LON_24_9342, testRoute);
        StopEntity furtherStop = createStop(STOP_2, 60.1686, 24.9324, testRoute);

        entityManager.persistAndFlush(nearestStop);
        entityManager.persistAndFlush(furtherStop);

        List<StopEntity> result = stopsRepository.getNearbyStops(LAT_60_1699, LON_24_9342, 2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(nearestStop);
        assertThat(result.get(1)).isEqualTo(furtherStop);
    }

    private StopEntity createStop(String name, double lat, double lon, RouteEntity route) {
        StopEntity stop = new StopEntity();
        stop.setName(name);
        stop.setLat(lat);
        stop.setLon(lon);
        stop.setRoute(route);
        return stop;
    }
}