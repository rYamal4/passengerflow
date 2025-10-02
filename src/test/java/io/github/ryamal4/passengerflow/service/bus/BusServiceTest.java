package io.github.ryamal4.passengerflow.service.bus;

import io.github.ryamal4.passengerflow.dto.BusDTO;
import io.github.ryamal4.passengerflow.model.Bus;
import io.github.ryamal4.passengerflow.model.Route;
import io.github.ryamal4.passengerflow.repository.IBusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusServiceTest {

    @Mock
    private IBusRepository busRepository;

    @InjectMocks
    private BusService busService;

    private Bus bus1;
    private Bus bus2;
    private Bus bus3;
    private Route route;

    @BeforeEach
    void setUp() {
        route = new Route();
        route.setId(1L);
        route.setName("Test Route");

        bus1 = new Bus();
        bus1.setId(1L);
        bus1.setModel("Bus Model 1");
        bus1.setRoute(route);

        bus2 = new Bus();
        bus2.setId(2L);
        bus2.setModel("Bus Model 2");
        bus2.setRoute(route);

        bus3 = new Bus();
        bus3.setId(3L);
        bus3.setModel("Bus Model 3");
        bus3.setRoute(route);
    }

    @Test
    void testGetAllBusesSuccess() {
        List<Bus> buses = List.of(bus1, bus2, bus3);

        when(busRepository.findAll()).thenReturn(buses);

        List<BusDTO> result = busService.getAllBuses();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getModel()).isEqualTo("Bus Model 1");
        assertThat(result.get(0).getRouteId()).isEqualTo(1L);
        assertThat(result.get(0).getRouteName()).isEqualTo("Test Route");

        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getModel()).isEqualTo("Bus Model 2");

        assertThat(result.get(2).getId()).isEqualTo(3L);
        assertThat(result.get(2).getModel()).isEqualTo("Bus Model 3");

        verify(busRepository).findAll();
    }

    @Test
    void testGetAllBusesEmptyResult() {
        when(busRepository.findAll()).thenReturn(List.of());

        List<BusDTO> result = busService.getAllBuses();

        assertThat(result).isEmpty();
        verify(busRepository).findAll();
    }

    @Test
    void testGetAllBusesConvertsToDTO() {
        List<Bus> buses = List.of(bus1);

        when(busRepository.findAll()).thenReturn(buses);

        List<BusDTO> result = busService.getAllBuses();

        assertThat(result).hasSize(1);
        BusDTO dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(bus1.getId());
        assertThat(dto.getModel()).isEqualTo(bus1.getModel());
        assertThat(dto.getRouteId()).isEqualTo(bus1.getRoute().getId());
        assertThat(dto.getRouteName()).isEqualTo(bus1.getRoute().getName());

        verify(busRepository).findAll();
    }
}
