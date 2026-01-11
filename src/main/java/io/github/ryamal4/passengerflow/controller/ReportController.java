package io.github.ryamal4.passengerflow.controller;

import io.github.ryamal4.passengerflow.service.report.IHeatmapReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final MediaType EXCEL_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final IHeatmapReportService heatmapReportService;

    @GetMapping("/heatmap")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> generateHeatmapReport(
            @RequestParam String route,
            @RequestParam(defaultValue = "true") boolean useWeather) {

        var pdfBytes = heatmapReportService.generateHeatmapReport(route, useWeather);
        var filename = String.format("heatmap_%s_%s.pdf",
                route.replaceAll("[^a-zA-Z0-9]", "_"),
                LocalDate.now().format(DATE_FORMATTER));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }

    @GetMapping("/heatmap/excel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> generateHeatmapExcelReport(
            @RequestParam String route,
            @RequestParam(defaultValue = "true") boolean useWeather) {

        var excelBytes = heatmapReportService.generateExcelReport(route, useWeather);
        var filename = String.format("heatmap_%s_%s.xlsx",
                route.replaceAll("[^a-zA-Z0-9]", "_"),
                LocalDate.now().format(DATE_FORMATTER));

        return ResponseEntity.ok()
                .contentType(EXCEL_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(excelBytes);
    }
}
