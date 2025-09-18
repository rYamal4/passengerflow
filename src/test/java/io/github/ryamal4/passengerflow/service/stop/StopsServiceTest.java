package io.github.ryamal4.passengerflow.service.stop;

import io.github.ryamal4.passengerflow.model.Route;
import io.github.ryamal4.passengerflow.model.Stop;
import io.github.ryamal4.passengerflow.repository.IStopsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StopsServiceTest {

    @Mock
    private IStopsRepository stopsRepository;

    @InjectMocks
    private StopsService stopsService;

    private Stop stop1;
    private Stop stop2;
    private Stop stop3;

    @BeforeEach
    void setUp() {
        Route route = new Route();
        route.setId(1L);
        route.setName("Test Route");

        stop1 = new Stop();
        stop1.setId(1L);
        stop1.setName("Stop 1");
        stop1.setLat(60.1695);
        stop1.setLon(24.9354);
        stop1.setRoute(route);

        stop2 = new Stop();
        stop2.setId(2L);
        stop2.setName("Stop 2");
        stop2.setLat(60.1699);
        stop2.setLon(24.9342);
        stop2.setRoute(route);

        stop3 = new Stop();
        stop3.setId(3L);
        stop3.setName("Stop 3");
        stop3.setLat(60.1700);
        stop3.setLon(24.9340);
        stop3.setRoute(route);
    }

    @Test
    void testGetNearbyStopsSuccess() {
        double lat = 60.1699;
        double lon = 24.9342;
        List<Stop> expectedStops = List.of(stop1, stop2, stop3);

        when(stopsRepository.findNearbyStops(lat, lon, 5)).thenReturn(expectedStops);

        List<Stop> result = stopsService.getNearbyStops(lat, lon);

        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(stop1, stop2, stop3);
        verify(stopsRepository).findNearbyStops(lat, lon, 5);
    }

    @Test
    void testGetNearbyStopsEmptyResult() {
        double lat = 0.0;
        double lon = 0.0;

        when(stopsRepository.findNearbyStops(lat, lon, 5)).thenReturn(List.of());

        List<Stop> result = stopsService.getNearbyStops(lat, lon);

        assertThat(result).isEmpty();
        verify(stopsRepository).findNearbyStops(lat, lon, 5);
    }

    @Test
    void testGetNearbyStopsUsesCorrectLimit() {
        double lat = 60.1699;
        double lon = 24.9342;
        List<Stop> fiveStops = List.of(stop1, stop2, stop3, stop1, stop2);

        when(stopsRepository.findNearbyStops(lat, lon, 5)).thenReturn(fiveStops);

        List<Stop> result = stopsService.getNearbyStops(lat, lon);

        assertThat(result).hasSize(5);
        verify(stopsRepository).findNearbyStops(lat, lon, 5);
    }

    @Test
    void testGetNearbyStopsWithNegativeCoordinates() {
        double lat = -60.1699;
        double lon = -24.9342;
        List<Stop> expectedStops = List.of(stop1);

        when(stopsRepository.findNearbyStops(lat, lon, 5)).thenReturn(expectedStops);

        List<Stop> result = stopsService.getNearbyStops(lat, lon);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(stop1);
        verify(stopsRepository).findNearbyStops(lat, lon, 5);
    }
}
