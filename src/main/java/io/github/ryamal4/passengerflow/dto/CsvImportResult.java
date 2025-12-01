package io.github.ryamal4.passengerflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CsvImportResult {
    private int successCount;
    private int failedCount;
    private List<String> errors;

    public boolean hasError() {
        return !errors.isEmpty();
    }
}
