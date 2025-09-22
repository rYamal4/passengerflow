package io.github.ryamal4.passengerflow.service.passenger;

import io.github.ryamal4.passengerflow.model.PassengerCount;
import io.github.ryamal4.passengerflow.model.dto.BusDTO;
import io.github.ryamal4.passengerflow.model.dto.PassengerCountDTO;
import io.github.ryamal4.passengerflow.model.dto.StopDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IPassengerCountService {
    PassengerCount createCount(PassengerCount count);

    PassengerCountDTO createCountFromDTO(PassengerCountDTO dto);

    Optional<PassengerCountDTO> getCountById(Long id);

    Page<PassengerCountDTO> getAllCounts(Pageable pageable);

    Page<PassengerCountDTO> getCountsByFilters(Long busId, Long stopId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    PassengerCountDTO updateCount(Long id, PassengerCountDTO dto);

    void deleteCount(Long id);

    List<BusDTO> getAllBuses();

    List<StopDTO> getAllStops();
}
