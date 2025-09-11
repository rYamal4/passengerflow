package io.github.ryamal4.passengerflow.service;

import io.github.ryamal4.passengerflow.model.PassengerCount;
import io.github.ryamal4.passengerflow.repository.IPassengerRepository;
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
        List<PassengerCount> recordsToProcess = passengerRepository
                .findByTimestampBetween(
                        date.atStartOfDay(),
                        date.plusDays(1).atStartOfDay()
                );

        if (recordsToProcess.isEmpty()) {
            return;
        }

        Map<Long, List<PassengerCount>> recordsByBus = recordsToProcess.stream()
                .collect(Collectors.groupingBy(entity -> entity.getBus().getId()));

        recordsByBus.forEach(this::calculateAndSaveCurrentPassengers);
    }

    private void calculateAndSaveCurrentPassengers(Long busId, List<PassengerCount> busRecords) {
        busRecords.sort(Comparator.comparing(PassengerCount::getTimestamp));

        int cumulativePassengerCount = 0;

        for (PassengerCount currentRecord : busRecords) {
            cumulativePassengerCount += (currentRecord.getEntered() - currentRecord.getExited());
            currentRecord.setCurrentPassengers(cumulativePassengerCount);
        }

        passengerRepository.saveAll(busRecords);
    }
}