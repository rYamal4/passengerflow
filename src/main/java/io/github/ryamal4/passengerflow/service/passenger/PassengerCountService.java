package io.github.ryamal4.passengerflow.service.passenger;

import io.github.ryamal4.passengerflow.dto.PassengerCountDTO;
import io.github.ryamal4.passengerflow.model.Bus;
import io.github.ryamal4.passengerflow.model.PassengerCount;
import io.github.ryamal4.passengerflow.model.Stop;
import io.github.ryamal4.passengerflow.repository.IBusRepository;
import io.github.ryamal4.passengerflow.repository.IPassengerCountRepository;
import io.github.ryamal4.passengerflow.repository.IStopsRepository;
import io.github.ryamal4.passengerflow.specification.PassengerCountSpecification;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    public PassengerCountDTO createCountFromDTO(PassengerCountDTO dto) {
        var count = convertToEntity(dto);
        var saved = passengerCountRepository.save(count);
        return convertToDTO(saved);
    }

    @Override
    public Optional<PassengerCountDTO> getCountById(Long id) {
        return passengerCountRepository.findById(id)
                .map(this::convertToDTO);
    }

    @Override
    public Page<PassengerCountDTO> getCountsByFilters(Long busId, Long stopId,
                                                      LocalDateTime startTime, LocalDateTime endTime,
                                                      Pageable pageable) {
        var spec = PassengerCountSpecification.withFilters(busId, stopId, startTime, endTime);
        Pageable pageableWithSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        return passengerCountRepository.findAll(spec, pageableWithSort)
                .map(this::convertToDTO);
    }

    @Override
    public PassengerCountDTO updateCount(Long id, PassengerCountDTO dto) {
        var existing = passengerCountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PassengerCount not found with id: " + id));

        existing.setEntered(dto.getEntered());
        existing.setExited(dto.getExited());
        existing.setTimestamp(dto.getTimestamp());

        if (!existing.getBus().getId().equals(dto.getBusId())) {
            var bus = findBusOrThrow(dto.getBusId());
            existing.setBus(bus);
        }

        if (!existing.getStop().getId().equals(dto.getStopId())) {
            var stop = findStopOrThrow(dto.getStopId());
            existing.setStop(stop);
        }

        var updated = passengerCountRepository.save(existing);
        return convertToDTO(updated);
    }

    @Override
    public void deleteCount(Long id) {
        if (!passengerCountRepository.existsById(id)) {
            throw new IllegalArgumentException("PassengerCount not found with id: " + id);
        }
        passengerCountRepository.deleteById(id);
    }


    private PassengerCountDTO convertToDTO(PassengerCount entity) {
        var dto = new PassengerCountDTO();

        dto.setId(entity.getId());
        dto.setBusId(entity.getBus().getId());
        dto.setStopId(entity.getStop().getId());
        dto.setEntered(entity.getEntered());
        dto.setExited(entity.getExited());
        dto.setTimestamp(entity.getTimestamp());
        dto.setBusModel(entity.getBus().getBusModel().getName());
        dto.setStopName(entity.getStop().getName());
        dto.setRouteName(entity.getStop().getRoute().getName());

        return dto;
    }

    private PassengerCount convertToEntity(PassengerCountDTO dto) {
        var entity = new PassengerCount();
        entity.setEntered(dto.getEntered());
        entity.setExited(dto.getExited());
        entity.setTimestamp(dto.getTimestamp());

        var bus = findBusOrThrow(dto.getBusId());
        entity.setBus(bus);

        var stop = findStopOrThrow(dto.getStopId());
        entity.setStop(stop);

        return entity;
    }

    private Bus findBusOrThrow(Long busId) {
        return busRepository.findById(busId)
                .orElseThrow(() -> new IllegalArgumentException("Bus not found with id: " + busId));
    }

    private Stop findStopOrThrow(Long stopId) {
        return stopsRepository.findById(stopId)
                .orElseThrow(() -> new IllegalArgumentException("Stop not found with id: " + stopId));
    }

}
