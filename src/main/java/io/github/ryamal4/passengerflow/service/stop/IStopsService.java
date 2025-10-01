package io.github.ryamal4.passengerflow.service.stop;

import io.github.ryamal4.passengerflow.dto.StopDTO;
import io.github.ryamal4.passengerflow.model.Stop;

import java.util.List;

public interface IStopsService {
    List<Stop> getNearbyStops(double lat, double lon);
    List<StopDTO> getAllStops();
}
