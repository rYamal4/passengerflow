package io.github.ryamal4.passengerflow.service.csv;

import io.github.ryamal4.passengerflow.dto.CsvImportResult;
import org.springframework.web.multipart.MultipartFile;

public interface ICsvImportService {
    CsvImportResult importBusModelsFromCsv(MultipartFile file);
}
