package io.github.ryamal4.passengerflow.service;

import io.github.ryamal4.passengerflow.model.RoutePredictionDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface ILoadPredictionService {
    public RoutePredictionDTO predictRouteLoad(LocalDateTime time, String routeName, String stop);

    public List<RoutePredictionDTO> predictTodayRouteLoads(String routeName);
}
