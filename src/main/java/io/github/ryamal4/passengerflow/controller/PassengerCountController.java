package io.github.ryamal4.passengerflow.controller;

import io.github.ryamal4.passengerflow.model.PassengerCount;
import io.github.ryamal4.passengerflow.model.dto.BusDTO;
import io.github.ryamal4.passengerflow.model.dto.PassengerCountDTO;
import io.github.ryamal4.passengerflow.model.dto.StopDTO;
import io.github.ryamal4.passengerflow.service.passenger.IPassengerCountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/passengers")
@CrossOrigin(origins = "*")
public class PassengerCountController {
    private final IPassengerCountService passengerCountService;

    public PassengerCountController(IPassengerCountService passengerCountService) {
        this.passengerCountService = passengerCountService;
    }

    @PostMapping
    public ResponseEntity<PassengerCount> createCount(@RequestBody @Valid PassengerCount count) {
        return ResponseEntity.status(HttpStatus.CREATED).body(passengerCountService.createCount(count));
    }

    @PostMapping("/dto")
    public ResponseEntity<PassengerCountDTO> createCountFromDTO(@RequestBody @Valid PassengerCountDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(passengerCountService.createCountFromDTO(dto));
    }

    @GetMapping
    public ResponseEntity<Page<PassengerCountDTO>> getAllCounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long busId,
            @RequestParam(required = false) Long stopId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        Pageable pageable = PageRequest.of(page, size);
        Page<PassengerCountDTO> result;

        if (busId != null || stopId != null || startTime != null || endTime != null) {
            result = passengerCountService.getCountsByFilters(busId, stopId, startTime, endTime, pageable);
        } else {
            result = passengerCountService.getAllCounts(pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PassengerCountDTO> getCountById(@PathVariable Long id) {
        return passengerCountService.getCountById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PassengerCountDTO> updateCount(@PathVariable Long id,
                                                         @RequestBody @Valid PassengerCountDTO dto) {
        try {
            PassengerCountDTO updated = passengerCountService.updateCount(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCount(@PathVariable Long id) {
        try {
            passengerCountService.deleteCount(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/buses")
    public ResponseEntity<List<BusDTO>> getAllBuses() {
        return ResponseEntity.ok(passengerCountService.getAllBuses());
    }

    @GetMapping("/stops")
    public ResponseEntity<List<StopDTO>> getAllStops() {
        return ResponseEntity.ok(passengerCountService.getAllStops());
    }
}
