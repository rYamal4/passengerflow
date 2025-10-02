package io.github.ryamal4.passengerflow.controller;

import io.github.ryamal4.passengerflow.dto.StopDTO;
import io.github.ryamal4.passengerflow.model.Stop;
import io.github.ryamal4.passengerflow.service.stop.IStopsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stops")
@CrossOrigin(origins = "*")
public class StopsController {
    private final IStopsService stopsService;

    public StopsController(IStopsService stopsService) {
        this.stopsService = stopsService;
    }

    @GetMapping
    public ResponseEntity<List<StopDTO>> getAllStops() {
        return ResponseEntity.ok(stopsService.getAllStops());
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<Stop>> getNearbyStops(@RequestParam double lat, @RequestParam double lon) {
        return ResponseEntity.ok(stopsService.getNearbyStops(lat, lon));
    }
}
