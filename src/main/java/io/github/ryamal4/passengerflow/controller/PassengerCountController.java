package io.github.ryamal4.passengerflow.controller;

import io.github.ryamal4.passengerflow.dto.PassengerCountDTO;
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

@RestController
@RequestMapping("/api/passengers")
public class PassengerCountController {
    private final IPassengerCountService passengerCountService;

    public PassengerCountController(IPassengerCountService passengerCountService) {
        this.passengerCountService = passengerCountService;
    }

    @PostMapping
    public ResponseEntity<PassengerCountDTO> createCount(@RequestBody @Valid PassengerCountDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(passengerCountService.createCountFromDTO(dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
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
        Page<PassengerCountDTO> result = passengerCountService.getCountsByFilters(busId, stopId, startTime, endTime, pageable);

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
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCount(@PathVariable Long id) {
        try {
            passengerCountService.deleteCount(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
