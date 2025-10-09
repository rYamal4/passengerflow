package io.github.ryamal4.passengerflow.controller;

import io.github.ryamal4.passengerflow.service.aggregation.IPassengerCountAggregationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;

@RestController
@RequestMapping("/api/aggregation")
public class AggregationController {
    private final IPassengerCountAggregationService aggregationService;

    public AggregationController(IPassengerCountAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @PostMapping
    public ResponseEntity<Void> performAggregation(@RequestParam Integer dayOfWeek) {
        aggregationService.performAggregation(DayOfWeek.of(dayOfWeek));
        return ResponseEntity.ok().build();
    }
}
