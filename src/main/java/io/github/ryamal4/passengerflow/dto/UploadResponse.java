package io.github.ryamal4.passengerflow.dto;

public record UploadResponse(
        boolean success,
        String message,
        String filename,
        BusModelDTO busModel
) {
}
