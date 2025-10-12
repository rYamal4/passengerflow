package io.github.ryamal4.passengerflow.service.prediction;

import io.github.ryamal4.passengerflow.dto.OccupancyPredictionDTO;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface IOccupancyPredictionService {
    Optional<OccupancyPredictionDTO> getPrediction(String routeName, String stopName, LocalTime time);

    List<OccupancyPredictionDTO> getTodayPredictions(String routeName);
}
