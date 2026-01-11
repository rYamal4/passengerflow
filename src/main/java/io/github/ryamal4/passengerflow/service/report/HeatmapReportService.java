package io.github.ryamal4.passengerflow.service.report;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import io.github.ryamal4.passengerflow.dto.HeatmapReportDTO;
import io.github.ryamal4.passengerflow.dto.OccupancyPredictionDTO;
import io.github.ryamal4.passengerflow.service.prediction.IOccupancyPredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    public byte[] generateExcelReport(String routeName, boolean useWeather) {
        var reportData = prepareReportData(routeName, useWeather);
        return createExcelWorkbook(reportData);
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

    private byte[] createExcelWorkbook(HeatmapReportDTO reportData) {
        try (var workbook = new XSSFWorkbook();
             var outputStream = new ByteArrayOutputStream()) {

            var sheet = workbook.createSheet("Тепловая карта");
            var styleCache = createCellStyles(workbook);

            createHeader(workbook, sheet, reportData);
            createHeatmapTable(workbook, sheet, reportData, styleCache);

            for (int i = 0; i <= reportData.getHours().size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate Excel report", e);
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    private void createHeader(Workbook workbook, Sheet sheet, HeatmapReportDTO reportData) {
        var headerStyle = workbook.createCellStyle();
        var headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 14);
        headerStyle.setFont(headerFont);

        var row0 = sheet.createRow(0);
        var titleCell = row0.createCell(0);
        titleCell.setCellValue("Отчет по маршруту " + reportData.getRouteName());
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        var row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue(reportData.getDayOfWeekName() + ", " +
                reportData.getReportDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        var row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("Сгенерировано: " +
                reportData.getGeneratedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));

        var row3 = sheet.createRow(3);
        row3.createCell(0).setCellValue("Учет погоды: " + (reportData.isWeatherEnabled() ? "+20% при дожде" : "Отключено"));
    }

    private void createHeatmapTable(Workbook workbook, Sheet sheet, HeatmapReportDTO reportData,
                                    Map<String, CellStyle> styleCache) {
        int startRow = 5;

        var headerStyle = workbook.createCellStyle();
        var headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        var headerRow = sheet.createRow(startRow);
        var stopHeaderCell = headerRow.createCell(0);
        stopHeaderCell.setCellValue("Остановка");
        stopHeaderCell.setCellStyle(headerStyle);

        for (int i = 0; i < reportData.getHours().size(); i++) {
            var cell = headerRow.createCell(i + 1);
            cell.setCellValue(reportData.getHours().get(i) + ":00");
            cell.setCellStyle(headerStyle);
        }

        int rowNum = startRow + 1;
        for (var stopName : reportData.getStopNames()) {
            var row = sheet.createRow(rowNum++);

            var stopCell = row.createCell(0);
            stopCell.setCellValue(stopName);

            var stopData = reportData.getHeatmapData().get(stopName);
            for (int i = 0; i < reportData.getHours().size(); i++) {
                var hour = reportData.getHours().get(i);
                var occupancy = stopData.get(hour);
                var cell = row.createCell(i + 1);

                if (occupancy != null) {
                    cell.setCellValue(Math.round(occupancy) + "%");
                } else {
                    cell.setCellValue("-");
                }

                var style = getOccupancyStyle(workbook, styleCache, occupancy);
                cell.setCellStyle(style);
            }
        }
    }

    private Map<String, CellStyle> createCellStyles(Workbook workbook) {
        return new HashMap<>();
    }

    private CellStyle getOccupancyStyle(Workbook workbook, Map<String, CellStyle> styleCache, Double occupancy) {
        var colorKey = getColorKey(occupancy);
        return styleCache.computeIfAbsent(colorKey, key -> {
            var style = (org.apache.poi.xssf.usermodel.XSSFCellStyle) workbook.createCellStyle();
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);

            var rgb = hexToRgb(key);
            var color = new XSSFColor(rgb, null);
            style.setFillForegroundColor(color);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            if (!key.equals("#9E9E9E")) {
                var font = workbook.createFont();
                font.setColor(IndexedColors.WHITE.getIndex());
                font.setBold(true);
                style.setFont(font);
            }

            return style;
        });
    }

    private String getColorKey(Double occupancy) {
        if (occupancy == null) return "#9E9E9E";
        if (occupancy < 50) return "#10b981";
        if (occupancy < 80) return "#f59e0b";
        if (occupancy < 100) return "#f97316";
        if (occupancy < 120) return "#ef4444";
        return "#dc2626";
    }

    private byte[] hexToRgb(String hex) {
        hex = hex.replace("#", "");
        return new byte[]{
                (byte) Integer.parseInt(hex.substring(0, 2), 16),
                (byte) Integer.parseInt(hex.substring(2, 4), 16),
                (byte) Integer.parseInt(hex.substring(4, 6), 16)
        };
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
