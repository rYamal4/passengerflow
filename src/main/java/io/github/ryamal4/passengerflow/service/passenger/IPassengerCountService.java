package io.github.ryamal4.passengerflow.service.passenger;

import io.github.ryamal4.passengerflow.dto.PassengerCountDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IPassengerCountService {
    PassengerCountDTO createCountFromDTO(PassengerCountDTO dto);

    Optional<PassengerCountDTO> getCountById(Long id);

    Page<PassengerCountDTO> getCountsByFilters(Long busId, Long stopId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    PassengerCountDTO updateCount(Long id, PassengerCountDTO dto);

    void deleteCount(Long id);
}
