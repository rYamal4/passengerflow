package io.github.ryamal4.passengerflow.service.report;

import io.github.ryamal4.passengerflow.dto.OccupancyPredictionDTO;
import io.github.ryamal4.passengerflow.service.prediction.IOccupancyPredictionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeatmapReportServiceTest {

    private static final String ROUTE_NAME = "7A";

    @Mock
    private IOccupancyPredictionService predictionService;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private HeatmapReportService reportService;

    @Test
    void testPrepareReportDataReturnsCorrectStructure() {
        var predictions = List.of(
                createPrediction("Stop A", 8, 45.0),
                createPrediction("Stop A", 9, 55.0),
                createPrediction("Stop B", 8, 70.0)
        );
        when(predictionService.getTodayPredictions(ROUTE_NAME, true)).thenReturn(predictions);

        var result = reportService.prepareReportData(ROUTE_NAME, true);

        assertThat(result.getRouteName()).isEqualTo(ROUTE_NAME);
        assertThat(result.isWeatherEnabled()).isTrue();
        assertThat(result.getStopNames()).containsExactly("Stop A", "Stop B");
        assertThat(result.getHours()).hasSize(13);
    }

    @Test
    void testPrepareReportDataBuildsHeatmapDataCorrectly() {
        var predictions = List.of(
                createPrediction("Stop A", 8, 45.0),
                createPrediction("Stop A", 9, 55.0)
        );
        when(predictionService.getTodayPredictions(ROUTE_NAME, false)).thenReturn(predictions);

        var result = reportService.prepareReportData(ROUTE_NAME, false);

        assertThat(result.getHeatmapData().get("Stop A").get(8)).isEqualTo(45.0);
        assertThat(result.getHeatmapData().get("Stop A").get(9)).isEqualTo(55.0);
        assertThat(result.isWeatherEnabled()).isFalse();
    }

    @Test
    void testPrepareReportDataHandlesEmptyPredictions() {
        when(predictionService.getTodayPredictions(ROUTE_NAME, true)).thenReturn(List.of());

        var result = reportService.prepareReportData(ROUTE_NAME, true);

        assertThat(result.getStopNames()).isEmpty();
        assertThat(result.getHeatmapData()).isEmpty();
    }

    @Test
    void testOccupancyColorHelperReturnsGrayForNull() {
        var helper = new HeatmapReportService.OccupancyColorHelper();

        assertThat(helper.getColor(null)).isEqualTo("#9E9E9E");
        assertThat(helper.getLabel(null)).isEqualTo("-");
    }

    @Test
    void testOccupancyColorHelperReturnsGreenForLowOccupancy() {
        var helper = new HeatmapReportService.OccupancyColorHelper();

        assertThat(helper.getColor(30.0)).isEqualTo("#10b981");
        assertThat(helper.getColor(49.9)).isEqualTo("#10b981");
    }

    @Test
    void testOccupancyColorHelperReturnsAmberForMediumOccupancy() {
        var helper = new HeatmapReportService.OccupancyColorHelper();

        assertThat(helper.getColor(50.0)).isEqualTo("#f59e0b");
        assertThat(helper.getColor(79.9)).isEqualTo("#f59e0b");
    }

    @Test
    void testOccupancyColorHelperReturnsOrangeForHighOccupancy() {
        var helper = new HeatmapReportService.OccupancyColorHelper();

        assertThat(helper.getColor(80.0)).isEqualTo("#f97316");
        assertThat(helper.getColor(99.9)).isEqualTo("#f97316");
    }

    @Test
    void testOccupancyColorHelperReturnsRedForCriticalOccupancy() {
        var helper = new HeatmapReportService.OccupancyColorHelper();

        assertThat(helper.getColor(100.0)).isEqualTo("#ef4444");
        assertThat(helper.getColor(119.9)).isEqualTo("#ef4444");
    }

    @Test
    void testOccupancyColorHelperReturnsDarkRedForOverload() {
        var helper = new HeatmapReportService.OccupancyColorHelper();

        assertThat(helper.getColor(120.0)).isEqualTo("#dc2626");
        assertThat(helper.getColor(150.0)).isEqualTo("#dc2626");
    }

    @Test
    void testOccupancyColorHelperFormatsLabel() {
        var helper = new HeatmapReportService.OccupancyColorHelper();

        assertThat(helper.getLabel(45.6)).isEqualTo("46%");
        assertThat(helper.getLabel(100.0)).isEqualTo("100%");
    }

    private OccupancyPredictionDTO createPrediction(String stopName, int hour, double occupancy) {
        return new OccupancyPredictionDTO(stopName, LocalTime.of(hour, 0), occupancy);
    }
}
