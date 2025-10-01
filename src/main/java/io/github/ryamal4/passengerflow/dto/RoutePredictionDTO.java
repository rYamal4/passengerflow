package io.github.ryamal4.passengerflow.dto;

import io.github.ryamal4.passengerflow.model.Route;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoutePredictionDTO {
    private Route route;
    private LocalDateTime departureTime;
    private Integer predictedLoad;
}