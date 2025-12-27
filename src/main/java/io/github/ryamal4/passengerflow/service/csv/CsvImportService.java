package io.github.ryamal4.passengerflow.service.csv;

import io.github.ryamal4.passengerflow.dto.CsvImportResult;
import io.github.ryamal4.passengerflow.model.BusModel;
import io.github.ryamal4.passengerflow.service.busmodel.IBusModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CsvImportService implements ICsvImportService {
    private final IBusModelService service;

    private CSVFormat createFormat() {
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .get();
    }

    @Override
    public CsvImportResult importStopsFromCsv(MultipartFile file) {
        List<BusModel> valid = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             var parser = new CSVParser(reader, createFormat())) {
            for (var csvRecord : parser) {
                processCsv(csvRecord, valid, errors);
            }

            var success = saveValid(valid);
            return new CsvImportResult(success.size(), valid.size() - success.size(), errors);
        } catch (IOException e) {
            log.error("Ошибка при чтении CSV файла", e);
            errors.add("Не удалось прочитать файл: " + e.getMessage());
            return new CsvImportResult(0, 0, errors);
        }
    }

    private void processCsv(CSVRecord csvRecord, List<BusModel> models, List<String> errors) {
        try {
            var model = new BusModel();

            model.setId(Long.valueOf(csvRecord.get("id")));
            model.setName(csvRecord.get("name"));
            model.setCapacity(Integer.valueOf(csvRecord.get("capacity")));

            models.add(model);
        } catch (NumberFormatException e) {
            errors.add("Строка " + csvRecord.getRecordNumber() + " : некорректные данные");
        }
    }

    private List<BusModel> saveValid(List<BusModel> valid) {
        List<BusModel> success = new ArrayList<>();
        for (var model : valid) {
            try {
                service.create(model);
                success.add(model);
            } catch (Exception e) {
                log.error("Не удалось сохранить модель автобуса {}", model.getName());
            }
        }

        return success;
    }
}
