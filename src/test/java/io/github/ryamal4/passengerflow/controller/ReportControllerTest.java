package io.github.ryamal4.passengerflow.controller;

import io.github.ryamal4.passengerflow.service.report.IHeatmapReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest extends AbstractControllerTest {

    @MockitoBean
    private IHeatmapReportService heatmapReportService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGenerateHeatmapReportReturnsPdf() throws Exception {
        var pdfBytes = "PDF content".getBytes();
        when(heatmapReportService.generateHeatmapReport("7A", true)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/reports/heatmap")
                        .param("route", "7A")
                        .param("useWeather", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("heatmap_7A_")));
    }

    @Test
    void testGenerateHeatmapReportDefaultsWeatherToTrue() throws Exception {
        var pdfBytes = "PDF content".getBytes();
        when(heatmapReportService.generateHeatmapReport("7A", true)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/reports/heatmap")
                        .param("route", "7A"))
                .andExpect(status().isOk());
    }

    @Test
    void testGenerateHeatmapReportSanitizesRouteNameInFilename() throws Exception {
        var pdfBytes = "PDF content".getBytes();
        when(heatmapReportService.generateHeatmapReport("Route/A", true)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/reports/heatmap")
                        .param("route", "Route/A"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("heatmap_Route_A_")));
    }
}
