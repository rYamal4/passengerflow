package io.github.ryamal4.passengerflow.controller;

import io.github.ryamal4.passengerflow.model.Stop;
import io.github.ryamal4.passengerflow.service.stop.IStopsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stops")
public class StopsController {
    private final IStopsService stopsService;

    public StopsController(IStopsService stopsService) {
        this.stopsService = stopsService;
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<Stop>> getNearbyStops(@RequestParam double lat, @RequestParam double lon) {
        return ResponseEntity.ok(stopsService.getNearbyStops(lat, lon));
    }
}
