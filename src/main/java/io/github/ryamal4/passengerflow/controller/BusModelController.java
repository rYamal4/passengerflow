package io.github.ryamal4.passengerflow.controller;

import io.github.ryamal4.passengerflow.dto.UploadResponse;
import io.github.ryamal4.passengerflow.service.busmodel.BusModelService;
import io.github.ryamal4.passengerflow.service.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/bus-models")
@RequiredArgsConstructor
public class BusModelController {
    private final FileService fileService;
    private final BusModelService busModelService;

    @PostMapping("/{id}/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UploadResponse> uploadFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            var busModel = busModelService.findById(id);
            var fileName = fileService.storeFile(file);

            busModel.setFileName(fileName);
            var updated = busModelService.update(busModel);
            var dto = busModelService.convertToDTO(updated);

            var response = new UploadResponse(
                    true,
                    "File uploaded successfully",
                    fileName,
                    dto
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            var response = new UploadResponse(
                    false,
                    "Failed to upload file: " + e.getMessage(),
                    null,
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        } catch (Exception e) {
            var response = new UploadResponse(
                    false,
                    "An error occurred: " + e.getMessage(),
                    null,
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }
}
