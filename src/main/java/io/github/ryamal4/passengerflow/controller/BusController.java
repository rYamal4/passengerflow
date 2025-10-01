package io.github.ryamal4.passengerflow.controller;

import io.github.ryamal4.passengerflow.dto.BusDTO;
import io.github.ryamal4.passengerflow.service.bus.IBusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/buses")
@CrossOrigin(origins = "*")
public class BusController {
    private final IBusService busService;

    public BusController(IBusService busService) {
        this.busService = busService;
    }

    @GetMapping
    public ResponseEntity<List<BusDTO>> getAllBuses() {
        return ResponseEntity.ok(busService.getAllBuses());
    }
}
