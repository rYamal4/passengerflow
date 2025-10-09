package io.github.ryamal4.passengerflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OccupancyPredictionDTO {
    private String stopName;
    private LocalTime time;
    private Double occupancyPercentage;
}
