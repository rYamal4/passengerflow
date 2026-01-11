package io.github.ryamal4.passengerflow.service.report;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import io.github.ryamal4.passengerflow.dto.HeatmapReportDTO;
import io.github.ryamal4.passengerflow.dto.OccupancyPredictionDTO;
import io.github.ryamal4.passengerflow.service.prediction.IOccupancyPredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class HeatmapReportService implements IHeatmapReportService {
    private static final ZoneId MOSCOW_ZONE_ID = ZoneId.of("Europe/Moscow");
    private static final List<Integer> REPORT_HOURS = IntStream.rangeClosed(6, 18).boxed().toList();

    private final IOccupancyPredictionService predictionService;
    private final TemplateEngine templateEngine;

    @Override
    public byte[] generateHeatmapReport(String routeName, boolean useWeather) {
        var reportData = prepareReportData(routeName, useWeather);
        var html = renderHtmlTemplate(reportData);
        return convertHtmlToPdf(html);
    }

    @Override
    public HeatmapReportDTO prepareReportData(String routeName, boolean useWeather) {
        var predictions = predictionService.getTodayPredictions(routeName, useWeather);
        var now = LocalDateTime.now(MOSCOW_ZONE_ID);

        var stopNames = predictions.stream()
                .map(OccupancyPredictionDTO::getStopName)
                .distinct()
                .toList();

        var heatmapData = new LinkedHashMap<String, java.util.Map<Integer, Double>>();
        for (var stopName : stopNames) {
            var stopData = predictions.stream()
                    .filter(p -> p.getStopName().equals(stopName))
                    .collect(Collectors.toMap(
                            p -> p.getTime().getHour(),
                            OccupancyPredictionDTO::getOccupancyPercentage,
                            (a, b) -> a
                    ));
            heatmapData.put(stopName, stopData);
        }

        return HeatmapReportDTO.builder()
                .routeName(routeName)
                .reportDate(now.toLocalDate())
                .generatedAt(now)
                .dayOfWeekName(now.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru")))
                .weatherEnabled(useWeather)
                .stopNames(stopNames)
                .hours(REPORT_HOURS)
                .heatmapData(heatmapData)
                .build();
    }

    private String renderHtmlTemplate(HeatmapReportDTO reportData) {
        var context = new Context();
        context.setVariable("report", reportData);
        context.setVariable("colorHelper", new OccupancyColorHelper());
        return templateEngine.process("reports/heatmap-report", context);
    }

    private byte[] convertHtmlToPdf(String html) {
        try (var outputStream = new ByteArrayOutputStream()) {
            var builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerFonts(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF report", e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private void registerFonts(PdfRendererBuilder builder) {
        var windowsFontPaths = new String[]{"arial.ttf", "arialbd.ttf"};
        var linuxFontPaths = new Path[]{
                Path.of("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
                Path.of("/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf")
        };

        for (var fontName : windowsFontPaths) {
            var fontPath = getWindowsFontPath(fontName);
            if (fontPath != null && Files.exists(fontPath)) {
                try {
                    builder.useFont(fontPath.toFile(), "Arial");
                    log.debug("Registered font: {}", fontPath);
                    return;
                } catch (Exception e) {
                    log.warn("Failed to register font {}: {}", fontPath, e.getMessage());
                }
            }
        }

        for (var fontPath : linuxFontPaths) {
            if (Files.exists(fontPath)) {
                try {
                    builder.useFont(fontPath.toFile(), "Arial");
                    log.debug("Registered font: {}", fontPath);
                    return;
                } catch (Exception e) {
                    log.warn("Failed to register font {}: {}", fontPath, e.getMessage());
                }
            }
        }

        log.warn("No suitable font found for Cyrillic support");
    }

    private Path getWindowsFontPath(String fontName) {
        var winDir = System.getenv("WINDIR");
        if (winDir != null) {
            return Path.of(winDir, "Fonts", fontName);
        }
        return null;
    }

    public static class OccupancyColorHelper {
        public String getColor(Double occupancy) {
            if (occupancy == null) return "#9E9E9E";
            if (occupancy < 50) return "#10b981";
            if (occupancy < 80) return "#f59e0b";
            if (occupancy < 100) return "#f97316";
            if (occupancy < 120) return "#ef4444";
            return "#dc2626";
        }

        public String getLabel(Double occupancy) {
            if (occupancy == null) return "-";
            return String.format("%.0f%%", occupancy);
        }
    }
}
