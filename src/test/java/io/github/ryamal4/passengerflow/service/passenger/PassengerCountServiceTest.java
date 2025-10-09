package io.github.ryamal4.passengerflow.service.passenger;

import io.github.ryamal4.passengerflow.dto.PassengerCountDTO;
import io.github.ryamal4.passengerflow.model.*;
import io.github.ryamal4.passengerflow.repository.IBusRepository;
import io.github.ryamal4.passengerflow.repository.IPassengerCountRepository;
import io.github.ryamal4.passengerflow.repository.IStopsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PassengerCountServiceTest {

    @Mock
    private IPassengerCountRepository passengerCountRepository;

    @Mock
    private IBusRepository busRepository;

    @Mock
    private IStopsRepository stopsRepository;

    @InjectMocks
    private PassengerCountService passengerCountService;

    private Bus bus;
    private Stop stop;
    private Route route;
    private PassengerCount passengerCount;
    private PassengerCountDTO passengerCountDTO;

    @BeforeEach
    void setUp() {
        route = new Route(1L, "Test Route", new ArrayList<>(), new ArrayList<>());
        bus = createBusWithModel(1L, 1L, "Test Bus", route);
        stop = createStop(1L, "Test Stop", 60.0, 24.0, route);
        passengerCount = new PassengerCount(1L, bus, stop, 10, 5, LocalDateTime.of(2025, 9, 12, 12, 0));
        passengerCountDTO = createPassengerCountDTO(1L, 1L, 10, 5, LocalDateTime.of(2025, 9, 12, 12, 0));
    }

    @Test
    void testCreateCountFromDTOSuccess() {
        when(busRepository.findById(1L)).thenReturn(Optional.of(bus));
        when(stopsRepository.findById(1L)).thenReturn(Optional.of(stop));
        when(passengerCountRepository.save(any(PassengerCount.class))).thenReturn(passengerCount);

        var result = passengerCountService.createCountFromDTO(passengerCountDTO);

        assertCountIsCorrect(result);
        verify(busRepository).findById(1L);
        verify(stopsRepository).findById(1L);
        verify(passengerCountRepository).save(any(PassengerCount.class));
    }

    @Test
    void testCreateCountFromDTOBusNotFound() {
        when(busRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerCountService.createCountFromDTO(passengerCountDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bus not found with id: 1");
        verify(busRepository).findById(1L);
        verify(stopsRepository, never()).findById(any());
        verify(passengerCountRepository, never()).save(any());
    }

    @Test
    void testCreateCountFromDTOStopNotFound() {
        when(busRepository.findById(1L)).thenReturn(Optional.of(bus));
        when(stopsRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerCountService.createCountFromDTO(passengerCountDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stop not found with id: 1");
        verify(busRepository).findById(1L);
        verify(stopsRepository).findById(1L);
        verify(passengerCountRepository, never()).save(any());
    }

    @Test
    void testGetCountByIdSuccess() {
        when(passengerCountRepository.findById(1L)).thenReturn(Optional.of(passengerCount));

        var result = passengerCountService.getCountById(1L);

        assertThat(result).isPresent();
        assertCountIsCorrect(result.get());
        verify(passengerCountRepository).findById(1L);
    }

    @Test
    void testGetCountByIdNotFound() {
        when(passengerCountRepository.findById(999L)).thenReturn(Optional.empty());

        var result = passengerCountService.getCountById(999L);

        assertThat(result).isEmpty();
        verify(passengerCountRepository).findById(999L);
    }

    @Test
    void testGetCountsByFilters() {
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(passengerCount), pageable, 1);
        when(passengerCountRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        var result = passengerCountService.getCountsByFilters(
                1L, 1L,
                LocalDateTime.of(2025, 9, 1, 0, 0),
                LocalDateTime.of(2025, 9, 30, 23, 59),
                pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertCountIsCorrect(result.getContent().get(0));
        verify(passengerCountRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testUpdateCountSuccess() {
        var updateDTO = createDefaultUpdateDTO();
        var updatedCount = new PassengerCount(1L, bus, stop, 15, 8, updateDTO.getTimestamp());
        when(passengerCountRepository.findById(1L)).thenReturn(Optional.of(passengerCount));
        when(passengerCountRepository.save(any(PassengerCount.class))).thenReturn(updatedCount);

        var result = passengerCountService.updateCount(1L, updateDTO);
        passengerCount = updatedCount;

        assertCountIsCorrect(result);
        verify(passengerCountRepository).findById(1L);
        verify(passengerCountRepository).save(any(PassengerCount.class));
    }

    @Test
    void testUpdateCountWithDifferentBus() {
        var newBus = createBusWithModel(2L, 2L, "New Bus", route);
        var updateDTO = createPassengerCountDTO(2L, 1L, 15, 8, LocalDateTime.of(2025, 9, 12, 13, 0));
        var updatedCount = new PassengerCount(1L, newBus, stop, 15, 8, updateDTO.getTimestamp());
        when(passengerCountRepository.findById(1L)).thenReturn(Optional.of(passengerCount));
        when(busRepository.findById(2L)).thenReturn(Optional.of(newBus));
        when(passengerCountRepository.save(any(PassengerCount.class))).thenReturn(updatedCount);

        var result = passengerCountService.updateCount(1L, updateDTO);
        passengerCount = updatedCount;
        bus = newBus;

        assertCountIsCorrect(result);
        verify(passengerCountRepository).findById(1L);
        verify(busRepository).findById(2L);
        verify(passengerCountRepository).save(any(PassengerCount.class));
    }

    @Test
    void testUpdateCountWithDifferentStop() {
        var newStop = createStop(2L, "New Stop", 61.0, 25.0, route);
        var updateDTO = createPassengerCountDTO(1L, 2L, 15, 8, LocalDateTime.of(2025, 9, 12, 13, 0));
        var updatedCount = new PassengerCount(1L, bus, newStop, 15, 8, updateDTO.getTimestamp());
        when(passengerCountRepository.findById(1L)).thenReturn(Optional.of(passengerCount));
        when(stopsRepository.findById(2L)).thenReturn(Optional.of(newStop));
        when(passengerCountRepository.save(any(PassengerCount.class))).thenReturn(updatedCount);

        var result = passengerCountService.updateCount(1L, updateDTO);
        passengerCount = updatedCount;
        stop = newStop;

        assertCountIsCorrect(result);
        verify(passengerCountRepository).findById(1L);
        verify(stopsRepository).findById(2L);
        verify(passengerCountRepository).save(any(PassengerCount.class));
    }

    @Test
    void testUpdateCountNotFound() {
        var updateDTO = createDefaultUpdateDTO();
        when(passengerCountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerCountService.updateCount(999L, updateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PassengerCount not found with id: 999");

        verify(passengerCountRepository).findById(999L);
        verify(passengerCountRepository, never()).save(any());
    }

    @Test
    void testUpdateCountBusNotFound() {
        var updateDTO = createPassengerCountDTO(999L, 1L, 15, 8, LocalDateTime.of(2025, 9, 12, 13, 0));
        when(passengerCountRepository.findById(1L)).thenReturn(Optional.of(passengerCount));
        when(busRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerCountService.updateCount(1L, updateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bus not found with id: 999");
        verify(passengerCountRepository).findById(1L);
        verify(busRepository).findById(999L);
        verify(passengerCountRepository, never()).save(any());
    }

    @Test
    void testUpdateCountStopNotFound() {
        var updateDTO = createPassengerCountDTO(1L, 999L, 15, 8, LocalDateTime.of(2025, 9, 12, 13, 0));
        when(passengerCountRepository.findById(1L)).thenReturn(Optional.of(passengerCount));
        when(stopsRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerCountService.updateCount(1L, updateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stop not found with id: 999");
        verify(passengerCountRepository).findById(1L);
        verify(stopsRepository).findById(999L);
        verify(passengerCountRepository, never()).save(any());
    }

    @Test
    void testDeleteCountSuccess() {
        when(passengerCountRepository.existsById(1L)).thenReturn(true);
        doNothing().when(passengerCountRepository).deleteById(1L);

        passengerCountService.deleteCount(1L);

        verify(passengerCountRepository).existsById(1L);
        verify(passengerCountRepository).deleteById(1L);
    }

    @Test
    void testDeleteCountNotFound() {
        when(passengerCountRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> passengerCountService.deleteCount(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PassengerCount not found with id: 999");
        verify(passengerCountRepository).existsById(999L);
        verify(passengerCountRepository, never()).deleteById(any());
    }

    private PassengerCountDTO createPassengerCountDTO(Long busId, Long stopId, Integer entered, Integer exited, LocalDateTime timestamp) {
        var dto = new PassengerCountDTO();
        dto.setBusId(busId);
        dto.setStopId(stopId);
        dto.setEntered(entered);
        dto.setExited(exited);
        dto.setTimestamp(timestamp);
        return dto;
    }

    private PassengerCountDTO createDefaultUpdateDTO() {
        return createPassengerCountDTO(1L, 1L, 15, 8, LocalDateTime.of(2025, 9, 12, 13, 0));
    }

    private Bus createBusWithModel(Long busId, Long modelId, String modelName, Route route) {
        var busModel = new BusModel(modelId, modelName, 50, new ArrayList<>());
        return new Bus(busId, busModel, route, new ArrayList<>());
    }

    private Stop createStop(Long id, String name, Double lat, Double lon, Route route) {
        return new Stop(id, name, lat, lon, route, new ArrayList<>());
    }

    private void assertCountIsCorrect(PassengerCountDTO dto) {
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(passengerCount.getId());
        assertThat(dto.getBusId()).isEqualTo(bus.getId());
        assertThat(dto.getStopId()).isEqualTo(stop.getId());
        assertThat(dto.getEntered()).isEqualTo(passengerCount.getEntered());
        assertThat(dto.getExited()).isEqualTo(passengerCount.getExited());
        assertThat(dto.getBusModel()).isEqualTo(bus.getBusModel().getName());
        assertThat(dto.getStopName()).isEqualTo(stop.getName());
        assertThat(dto.getRouteName()).isEqualTo(route.getName());
    }
}