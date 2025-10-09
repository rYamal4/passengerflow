package io.github.ryamal4.passengerflow.service.stop;

import io.github.ryamal4.passengerflow.dto.StopDTO;

import java.util.List;

public interface IStopsService {
    List<StopDTO> getNearbyStops(double lat, double lon);

    List<StopDTO> getAllStops();
}
