package io.github.ryamal4.passengerflow.service.passenger;

import io.github.ryamal4.passengerflow.model.*;
import io.github.ryamal4.passengerflow.repository.IBusRepository;
import io.github.ryamal4.passengerflow.repository.IPassengerCountRepository;
import io.github.ryamal4.passengerflow.repository.IStopsRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PassengerCountService implements IPassengerCountService {
    private final IPassengerCountRepository passengerCountRepository;
    private final IBusRepository busRepository;
    private final IStopsRepository stopsRepository;

    public PassengerCountService(IPassengerCountRepository passengerCountRepository,
                                 IBusRepository busRepository,
                                 IStopsRepository stopsRepository) {
        this.passengerCountRepository = passengerCountRepository;
        this.busRepository = busRepository;
        this.stopsRepository = stopsRepository;
    }

    @Override
    public PassengerCount createCount(PassengerCount count) {
        return passengerCountRepository.save(count);
    }

    @Override
    public PassengerCountDTO createCountFromDTO(PassengerCountDTO dto) {
        PassengerCount count = convertToEntity(dto);
        PassengerCount saved = passengerCountRepository.save(count);
        return convertToDTO(saved);
    }

    @Override
    public Optional<PassengerCountDTO> getCountById(Long id) {
        return passengerCountRepository.findById(id)
                .map(this::convertToDTO);
    }

    @Override
    public Page<PassengerCountDTO> getAllCounts(Pageable pageable) {
        return passengerCountRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    @Override
    public Page<PassengerCountDTO> getCountsByFilters(Long busId, Long stopId,
                                                      LocalDateTime startTime, LocalDateTime endTime,
                                                      Pageable pageable) {
        return passengerCountRepository.findByFilters(busId, stopId, startTime, endTime, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public PassengerCountDTO updateCount(Long id, PassengerCountDTO dto) {
        PassengerCount existing = passengerCountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PassengerCount not found with id: " + id));

        // Update fields
        existing.setEntered(dto.getEntered());
        existing.setExited(dto.getExited());
        existing.setTimestamp(dto.getTimestamp());

        // Update relationships if changed
        if (!existing.getBus().getId().equals(dto.getBusId())) {
            Bus bus = busRepository.findById(dto.getBusId())
                    .orElseThrow(() -> new RuntimeException("Bus not found with id: " + dto.getBusId()));
            existing.setBus(bus);
        }

        if (!existing.getStop().getId().equals(dto.getStopId())) {
            Stop stop = stopsRepository.findById(dto.getStopId())
                    .orElseThrow(() -> new RuntimeException("Stop not found with id: " + dto.getStopId()));
            existing.setStop(stop);
        }

        PassengerCount updated = passengerCountRepository.save(existing);
        return convertToDTO(updated);
    }

    @Override
    public void deleteCount(Long id) {
        if (!passengerCountRepository.existsById(id)) {
            throw new IllegalStateException("PassengerCount not found with id: " + id);
        }
        passengerCountRepository.deleteById(id);
    }

    @Override
    public List<BusDTO> getAllBuses() {
        return busRepository.findAll().stream()
                .map(this::convertBusToDTO)
                .toList();
    }

    @Override
    public List<StopDTO> getAllStops() {
        return stopsRepository.findAll().stream()
                .map(this::convertStopToDTO)
                .toList();
    }

    private PassengerCountDTO convertToDTO(PassengerCount entity) {
        PassengerCountDTO dto = new PassengerCountDTO();
        dto.setId(entity.getId());
        dto.setBusId(entity.getBus().getId());
        dto.setStopId(entity.getStop().getId());
        dto.setEntered(entity.getEntered());
        dto.setExited(entity.getExited());
        dto.setTimestamp(entity.getTimestamp());

        // Additional display fields
        dto.setBusModel(entity.getBus().getModel());
        dto.setStopName(entity.getStop().getName());
        dto.setRouteName(entity.getStop().getRoute().getName());

        return dto;
    }

    private PassengerCount convertToEntity(PassengerCountDTO dto) {
        PassengerCount entity = new PassengerCount();
        entity.setEntered(dto.getEntered());
        entity.setExited(dto.getExited());
        entity.setTimestamp(dto.getTimestamp());

        Bus bus = busRepository.findById(dto.getBusId())
                .orElseThrow(() -> new RuntimeException("Bus not found with id: " + dto.getBusId()));
        entity.setBus(bus);

        Stop stop = stopsRepository.findById(dto.getStopId())
                .orElseThrow(() -> new RuntimeException("Stop not found with id: " + dto.getStopId()));
        entity.setStop(stop);

        return entity;
    }

    private BusDTO convertBusToDTO(Bus entity) {
        BusDTO dto = new BusDTO();
        dto.setId(entity.getId());
        dto.setModel(entity.getModel());
        dto.setRouteId(entity.getRoute().getId());
        dto.setRouteName(entity.getRoute().getName());
        return dto;
    }

    private StopDTO convertStopToDTO(Stop entity) {
        StopDTO dto = new StopDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setLat(entity.getLat());
        dto.setLon(entity.getLon());
        dto.setRouteId(entity.getRoute().getId());
        dto.setRouteName(entity.getRoute().getName());
        return dto;
    }
}
