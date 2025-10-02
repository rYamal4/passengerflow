package io.github.ryamal4.passengerflow.service.passenger;

import io.github.ryamal4.passengerflow.dto.PassengerCountDTO;
import io.github.ryamal4.passengerflow.model.Bus;
import io.github.ryamal4.passengerflow.model.PassengerCount;
import io.github.ryamal4.passengerflow.model.Route;
import io.github.ryamal4.passengerflow.model.Stop;
import io.github.ryamal4.passengerflow.repository.IBusRepository;
import io.github.ryamal4.passengerflow.repository.IPassengerCountRepository;
import io.github.ryamal4.passengerflow.repository.IStopsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
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
        route = new Route();
        route.setId(1L);
        route.setName("Test Route");

        bus = new Bus();
        bus.setId(1L);
        bus.setModel("Test Bus");
        bus.setRoute(route);

        stop = new Stop();
        stop.setId(1L);
        stop.setName("Test Stop");
        stop.setLat(60.0);
        stop.setLon(24.0);
        stop.setRoute(route);

        passengerCount = new PassengerCount();
        passengerCount.setId(1L);
        passengerCount.setBus(bus);
        passengerCount.setStop(stop);
        passengerCount.setEntered(10);
        passengerCount.setExited(5);
        passengerCount.setTimestamp(LocalDateTime.of(2025, 9, 12, 12, 0));

        passengerCountDTO = new PassengerCountDTO();
        passengerCountDTO.setBusId(1L);
        passengerCountDTO.setStopId(1L);
        passengerCountDTO.setEntered(10);
        passengerCountDTO.setExited(5);
        passengerCountDTO.setTimestamp(LocalDateTime.of(2025, 9, 12, 12, 0));
    }

    @Test
    void testCreateCountSuccess() {
        PassengerCount inputCount = new PassengerCount();
        inputCount.setEntered(10);
        inputCount.setExited(5);

        PassengerCount savedCount = new PassengerCount();
        savedCount.setId(1L);
        savedCount.setEntered(10);
        savedCount.setExited(5);

        when(passengerCountRepository.save(inputCount)).thenReturn(savedCount);

        PassengerCount result = passengerCountService.createCount(inputCount);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEntered()).isEqualTo(10);
        assertThat(result.getExited()).isEqualTo(5);
    }

    @Test
    void testCreateCountFromDTOSuccess() {
        when(busRepository.findById(1L)).thenReturn(Optional.of(bus));
        when(stopsRepository.findById(1L)).thenReturn(Optional.of(stop));
        when(passengerCountRepository.save(any(PassengerCount.class))).thenReturn(passengerCount);

        PassengerCountDTO result = passengerCountService.createCountFromDTO(passengerCountDTO);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getBusId()).isEqualTo(1L);
        assertThat(result.getStopId()).isEqualTo(1L);
        assertThat(result.getEntered()).isEqualTo(10);
        assertThat(result.getExited()).isEqualTo(5);
        assertThat(result.getBusModel()).isEqualTo("Test Bus");
        assertThat(result.getStopName()).isEqualTo("Test Stop");
        assertThat(result.getRouteName()).isEqualTo("Test Route");

        verify(busRepository).findById(1L);
        verify(stopsRepository).findById(1L);
        verify(passengerCountRepository).save(any(PassengerCount.class));
    }

    @Test
    void testCreateCountFromDTOBusNotFound() {
        when(busRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerCountService.createCountFromDTO(passengerCountDTO))
                .isInstanceOf(IllegalStateException.class)
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
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stop not found with id: 1");

        verify(busRepository).findById(1L);
        verify(stopsRepository).findById(1L);
        verify(passengerCountRepository, never()).save(any());
    }

    @Test
    void testGetCountByIdSuccess() {
        when(passengerCountRepository.findById(1L)).thenReturn(Optional.of(passengerCount));

        Optional<PassengerCountDTO> result = passengerCountService.getCountById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getBusId()).isEqualTo(1L);
        assertThat(result.get().getStopId()).isEqualTo(1L);
        assertThat(result.get().getEntered()).isEqualTo(10);
        assertThat(result.get().getExited()).isEqualTo(5);

        verify(passengerCountRepository).findById(1L);
    }

    @Test
    void testGetCountByIdNotFound() {
        when(passengerCountRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<PassengerCountDTO> result = passengerCountService.getCountById(999L);

        assertThat(result).isEmpty();
        verify(passengerCountRepository).findById(999L);
    }

    @Test
    void testGetCountsByFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<PassengerCount> page = new PageImpl<>(List.of(passengerCount), pageable, 1);

        when(passengerCountRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<PassengerCountDTO> result = passengerCountService.getCountsByFilters(
                1L, 1L,
                LocalDateTime.of(2025, 9, 1, 0, 0),
                LocalDateTime.of(2025, 9, 30, 23, 59),
                pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);

        verify(passengerCountRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testUpdateCountSuccess() {
        PassengerCountDTO updateDTO = new PassengerCountDTO();
        updateDTO.setBusId(1L);
        updateDTO.setStopId(1L);
        updateDTO.setEntered(15);
        updateDTO.setExited(8);
        updateDTO.setTimestamp(LocalDateTime.of(2025, 9, 12, 13, 0));

        PassengerCount updatedCount = new PassengerCount();
        updatedCount.setId(1L);
        updatedCount.setBus(bus);
        updatedCount.setStop(stop);
        updatedCount.setEntered(15);
        updatedCount.setExited(8);
        updatedCount.setTimestamp(updateDTO.getTimestamp());

        when(passengerCountRepository.findById(1L)).thenReturn(Optional.of(passengerCount));
        when(passengerCountRepository.save(any(PassengerCount.class))).thenReturn(updatedCount);

        PassengerCountDTO result = passengerCountService.updateCount(1L, updateDTO);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEntered()).isEqualTo(15);
        assertThat(result.getExited()).isEqualTo(8);

        verify(passengerCountRepository).findById(1L);
        verify(passengerCountRepository).save(any(PassengerCount.class));
    }

    @Test
    void testUpdateCountWithDifferentBus() {
        Bus newBus = new Bus();
        newBus.setId(2L);
        newBus.setModel("New Bus");
        newBus.setRoute(route);

        PassengerCountDTO updateDTO = new PassengerCountDTO();
        updateDTO.setBusId(2L);
        updateDTO.setStopId(1L);
        updateDTO.setEntered(15);
        updateDTO.setExited(8);
        updateDTO.setTimestamp(LocalDateTime.of(2025, 9, 12, 13, 0));

        PassengerCount updatedCount = new PassengerCount();
        updatedCount.setId(1L);
        updatedCount.setBus(newBus);
        updatedCount.setStop(stop);
        updatedCount.setEntered(15);
        updatedCount.setExited(8);
        updatedCount.setTimestamp(updateDTO.getTimestamp());

        when(passengerCountRepository.findById(1L)).thenReturn(Optional.of(passengerCount));
        when(busRepository.findById(2L)).thenReturn(Optional.of(newBus));
        when(passengerCountRepository.save(any(PassengerCount.class))).thenReturn(updatedCount);

        PassengerCountDTO result = passengerCountService.updateCount(1L, updateDTO);

        assertThat(result).isNotNull();
        assertThat(result.getBusId()).isEqualTo(2L);

        verify(passengerCountRepository).findById(1L);
        verify(busRepository).findById(2L);
        verify(passengerCountRepository).save(any(PassengerCount.class));
    }

    @Test
    void testUpdateCountWithDifferentStop() {
        Stop newStop = new Stop();
        newStop.setId(2L);
        newStop.setName("New Stop");
        newStop.setLat(61.0);
        newStop.setLon(25.0);
        newStop.setRoute(route);

        PassengerCountDTO updateDTO = new PassengerCountDTO();
        updateDTO.setBusId(1L);
        updateDTO.setStopId(2L);
        updateDTO.setEntered(15);
        updateDTO.setExited(8);
        updateDTO.setTimestamp(LocalDateTime.of(2025, 9, 12, 13, 0));

        PassengerCount updatedCount = new PassengerCount();
        updatedCount.setId(1L);
        updatedCount.setBus(bus);
        updatedCount.setStop(newStop);
        updatedCount.setEntered(15);
        updatedCount.setExited(8);
        updatedCount.setTimestamp(updateDTO.getTimestamp());

        when(passengerCountRepository.findById(1L)).thenReturn(Optional.of(passengerCount));
        when(stopsRepository.findById(2L)).thenReturn(Optional.of(newStop));
        when(passengerCountRepository.save(any(PassengerCount.class))).thenReturn(updatedCount);

        PassengerCountDTO result = passengerCountService.updateCount(1L, updateDTO);

        assertThat(result).isNotNull();
        assertThat(result.getStopId()).isEqualTo(2L);

        verify(passengerCountRepository).findById(1L);
        verify(stopsRepository).findById(2L);
        verify(passengerCountRepository).save(any(PassengerCount.class));
    }

    @Test
    void testUpdateCountNotFound() {
        PassengerCountDTO updateDTO = new PassengerCountDTO();
        updateDTO.setBusId(1L);
        updateDTO.setStopId(1L);
        updateDTO.setEntered(15);
        updateDTO.setExited(8);
        updateDTO.setTimestamp(LocalDateTime.of(2025, 9, 12, 13, 0));

        when(passengerCountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerCountService.updateCount(999L, updateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PassengerCount not found with id: 999");

        verify(passengerCountRepository).findById(999L);
        verify(passengerCountRepository, never()).save(any());
    }

    @Test
    void testUpdateCountBusNotFound() {
        PassengerCountDTO updateDTO = new PassengerCountDTO();
        updateDTO.setBusId(999L);
        updateDTO.setStopId(1L);
        updateDTO.setEntered(15);
        updateDTO.setExited(8);
        updateDTO.setTimestamp(LocalDateTime.of(2025, 9, 12, 13, 0));

        when(passengerCountRepository.findById(1L)).thenReturn(Optional.of(passengerCount));
        when(busRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerCountService.updateCount(1L, updateDTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bus not found with id: 999");

        verify(passengerCountRepository).findById(1L);
        verify(busRepository).findById(999L);
        verify(passengerCountRepository, never()).save(any());
    }

    @Test
    void testUpdateCountStopNotFound() {
        PassengerCountDTO updateDTO = new PassengerCountDTO();
        updateDTO.setBusId(1L);
        updateDTO.setStopId(999L);
        updateDTO.setEntered(15);
        updateDTO.setExited(8);
        updateDTO.setTimestamp(LocalDateTime.of(2025, 9, 12, 13, 0));

        when(passengerCountRepository.findById(1L)).thenReturn(Optional.of(passengerCount));
        when(stopsRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passengerCountService.updateCount(1L, updateDTO))
                .isInstanceOf(IllegalStateException.class)
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
}