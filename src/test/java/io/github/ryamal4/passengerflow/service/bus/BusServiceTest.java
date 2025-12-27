package io.github.ryamal4.passengerflow.service.bus;

import io.github.ryamal4.passengerflow.dto.BusDTO;
import io.github.ryamal4.passengerflow.model.Bus;
import io.github.ryamal4.passengerflow.model.BusModel;
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

    @BeforeEach
    void setUp() {
        Route route = createRoute();

        bus1 = createBus(1L, createBusModel(1L, "Bus Model 1", 50), route);
        bus2 = createBus(2L, createBusModel(2L, "Bus Model 2", 60), route);
        bus3 = createBus(3L, createBusModel(3L, "Bus Model 3", 70), route);
    }

    @Test
    void testGetAllBusesSuccess() {
        var buses = List.of(bus1, bus2, bus3);
        when(busRepository.findAll()).thenReturn(buses);

        var result = busService.getAllBuses();

        assertThat(result).hasSize(3);
        assertDtoIsCorrect(result.get(0), bus1);
        assertDtoIsCorrect(result.get(1), bus2);
        assertDtoIsCorrect(result.get(2), bus3);
        verify(busRepository).findAll();
    }

    @Test
    void testGetAllBusesEmptyList() {
        when(busRepository.findAll()).thenReturn(List.of());

        var result = busService.getAllBuses();

        assertThat(result).isEmpty();
        verify(busRepository).findAll();
    }

    private Route createRoute() {
        var route = new Route();
        route.setId(1L);
        route.setName("Test Route");
        return route;
    }

    private BusModel createBusModel(Long id, String name, Integer capacity) {
        BusModel busModel = new BusModel();
        busModel.setId(id);
        busModel.setName(name);
        busModel.setCapacity(capacity);
        return busModel;
    }

    private Bus createBus(Long id, BusModel busModel, Route route) {
        Bus bus = new Bus();
        bus.setId(id);
        bus.setBusModel(busModel);
        bus.setRoute(route);
        return bus;
    }

    private void assertDtoIsCorrect(BusDTO dto, Bus bus) {
        assertThat(dto.getId()).isEqualTo(bus.getId());
        assertThat(dto.getBusModelId()).isEqualTo(bus.getBusModel().getId());
        assertThat(dto.getBusModelName()).isEqualTo(bus.getBusModel().getName());
        assertThat(dto.getBusModelCapacity()).isEqualTo(bus.getBusModel().getCapacity());
        assertThat(dto.getRouteId()).isEqualTo(bus.getRoute().getId());
        assertThat(dto.getRouteName()).isEqualTo(bus.getRoute().getName());
    }
}
