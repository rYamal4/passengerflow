package io.github.ryamal4.passengerflow.service;

import io.github.ryamal4.passengerflow.persistence.entities.PassengerCountEntity;
import io.github.ryamal4.passengerflow.persistence.repository.IPassengerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PassengerCountCalculationService {

    private final IPassengerRepository passengerRepository;

    public PassengerCountCalculationService(IPassengerRepository passengerRepository) {
        this.passengerRepository = passengerRepository;
    }

    @Transactional
    public void calculateCurrentPassengersForDate(LocalDate date) {
        List<PassengerCountEntity> recordsToProcess = passengerRepository
                .findByTimestampBetween(
                        date.atStartOfDay(),
                        date.plusDays(1).atStartOfDay()
                );

        if (recordsToProcess.isEmpty()) {
            return;
        }

        Map<Long, List<PassengerCountEntity>> recordsByBus = recordsToProcess.stream()
                .collect(Collectors.groupingBy(entity -> entity.getBus().getId()));

        recordsByBus.forEach(this::calculateAndSaveCurrentPassengers);
    }

    private void calculateAndSaveCurrentPassengers(Long busId, List<PassengerCountEntity> busRecords) {
        busRecords.sort(Comparator.comparing(PassengerCountEntity::getTimestamp));

        int cumulativePassengerCount = 0;

        for (PassengerCountEntity currentRecord : busRecords) {
            cumulativePassengerCount += (currentRecord.getEntered() - currentRecord.getExited());
            currentRecord.setCurrentPassengers(cumulativePassengerCount);
        }

        passengerRepository.saveAll(busRecords);
    }
}