package io.github.ryamal4.passengerflow.service.passenger;

import io.github.ryamal4.passengerflow.model.PassengerCount;
import io.github.ryamal4.passengerflow.repository.IPassengerCountRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class PassengerCountService implements IPassengerCountService {
    private final IPassengerCountRepository passengerCountRepository;

    public PassengerCountService(IPassengerCountRepository passengerCountRepository) {
        this.passengerCountRepository = passengerCountRepository;
    }

    @Override
    public PassengerCount createCount(PassengerCount count) {
        return passengerCountRepository.save(count);
    }
}
