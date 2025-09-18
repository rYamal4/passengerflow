package io.github.ryamal4.passengerflow.controller;

import io.github.ryamal4.passengerflow.model.PassengerCount;
import io.github.ryamal4.passengerflow.service.passenger.IPassengerCountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/passengers")
public class PassengersCountController {
    private final IPassengerCountService passengerCountService;

    public PassengersCountController(IPassengerCountService passengerCountService) {
        this.passengerCountService = passengerCountService;
    }

    @PostMapping
    public ResponseEntity<PassengerCount> createCount(@RequestBody @Valid PassengerCount count) {
        return ResponseEntity.status(HttpStatus.CREATED).body(passengerCountService.createCount(count));
    }
}
