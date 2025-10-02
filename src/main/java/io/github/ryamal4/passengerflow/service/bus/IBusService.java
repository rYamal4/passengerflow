package io.github.ryamal4.passengerflow.service.bus;

import io.github.ryamal4.passengerflow.dto.BusDTO;

import java.util.List;

public interface IBusService {
    List<BusDTO> getAllBuses();
}
