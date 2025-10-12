package io.github.ryamal4.passengerflow.controller;

import io.github.ryamal4.passengerflow.dto.OccupancyPredictionDTO;
import io.github.ryamal4.passengerflow.service.prediction.IOccupancyPredictionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/predictions")
public class OccupancyPredictionController {
    private final IOccupancyPredictionService predictionService;

    public OccupancyPredictionController(IOccupancyPredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @GetMapping
    public ResponseEntity<List<OccupancyPredictionDTO>> getPredictions(
            @RequestParam String route,
            @RequestParam(required = false) String stop,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time) {

        if (stop != null && time != null) {
            return predictionService.getPrediction(route, stop, time)
                    .map(prediction -> ResponseEntity.ok(List.of(prediction)))
                    .orElse(ResponseEntity.notFound().build());
        } else {
            var predictions = predictionService.getTodayPredictions(route);
            return ResponseEntity.ok(predictions);
        }
    }
}
