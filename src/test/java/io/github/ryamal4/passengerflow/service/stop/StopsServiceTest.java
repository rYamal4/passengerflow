package io.github.ryamal4.passengerflow.service.stop;

import io.github.ryamal4.passengerflow.dto.StopDTO;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StopsServiceTest {
    public static final int FIVE_LIMIT = 5;
    private static final double TEST_LAT = 0;
    private static final double TEST_LON = 0;

    @Mock
    private IStopsRepository stopsRepository;

    @InjectMocks
    private StopsService stopsService;

    private Stop stop1;
    private Stop stop2;
    private Stop stop3;

    @BeforeEach
    void setUp() {
        Route route = createRoute();

        stop1 = createStop(1L, "Stop 1", 0.1, 0.1, route);
        stop2 = createStop(2L, "Stop 2", 0.2, 0.2, route);
        stop3 = createStop(3L, "Stop 3", 0.3, 0.3, route);
    }

    @Test
    void testGetNearbyStopsUsesLimitOfFive() {
        stopsService.getNearbyStops(TEST_LAT, TEST_LON);

        verify(stopsRepository).findNearbyStops(TEST_LAT, TEST_LON, FIVE_LIMIT);
    }

    @Test
    void testGetNearbyStopsReturnsDataFromRepository() {
        var stops = List.of(stop1, stop2, stop3);
        when(stopsRepository.findNearbyStops(TEST_LAT, TEST_LON, FIVE_LIMIT)).thenReturn(stops);

        var result = stopsService.getNearbyStops(TEST_LAT, TEST_LON);

        assertThat(result).hasSize(3);
        assertDtoIsCorrect(result.get(0), stop1);
        assertDtoIsCorrect(result.get(1), stop2);
        assertDtoIsCorrect(result.get(2), stop3);
    }

    @Test
    void testGetNearbyStopsThrowsExceptionForInvalidLat() {
        assertThrows(IllegalArgumentException.class, () -> stopsService.getNearbyStops(91, TEST_LON));
        assertThrows(IllegalArgumentException.class, () -> stopsService.getNearbyStops(-91, TEST_LON));
    }

    @Test
    void testGetNearbyStopsThrowsExceptionForInvalidLon() {
        assertThrows(IllegalArgumentException.class, () -> stopsService.getNearbyStops(TEST_LAT, 181));
        assertThrows(IllegalArgumentException.class, () -> stopsService.getNearbyStops(TEST_LAT, -181));
    }

    @Test
    void testGetAllStopsSuccess() {
        var stops = List.of(stop1, stop2, stop3);
        when(stopsRepository.findAll()).thenReturn(stops);

        var result = stopsService.getAllStops();

        verify(stopsRepository).findAll();
        assertThat(result).hasSize(3);
        assertDtoIsCorrect(result.get(0), stop1);
        assertDtoIsCorrect(result.get(1), stop2);
        assertDtoIsCorrect(result.get(2), stop3);
    }

    private Route createRoute() {
        var route = new Route();
        route.setId(1L);
        route.setName("Test Route");
        return route;
    }

    private Stop createStop(Long id, String name, Double lat, Double lon, Route route) {
        var stop = new Stop();
        stop.setId(id);
        stop.setName(name);
        stop.setLat(lat);
        stop.setLon(lon);
        stop.setRoute(route);
        return stop;
    }

    private void assertDtoIsCorrect(StopDTO dto, Stop stop) {
        assertThat(dto.getId()).isEqualTo(stop.getId());
        assertThat(dto.getName()).isEqualTo(stop.getName());
        assertThat(dto.getLat()).isEqualTo(stop.getLat());
        assertThat(dto.getLon()).isEqualTo(stop.getLon());
        assertThat(dto.getRouteId()).isEqualTo(stop.getRoute().getId());
        assertThat(dto.getRouteName()).isEqualTo(stop.getRoute().getName());
    }
}
