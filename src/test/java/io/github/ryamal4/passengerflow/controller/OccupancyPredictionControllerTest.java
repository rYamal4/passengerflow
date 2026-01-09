package io.github.ryamal4.passengerflow.controller;

import io.github.ryamal4.passengerflow.dto.OccupancyPredictionDTO;
import io.github.ryamal4.passengerflow.service.prediction.IOccupancyPredictionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OccupancyPredictionController.class)
@AutoConfigureMockMvc(addFilters = false)
class OccupancyPredictionControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IOccupancyPredictionService predictionService;

    @Test
    void testGetSinglePredictionReturnsOk() throws Exception {
        var prediction = new OccupancyPredictionDTO("Central Station", LocalTime.of(15, 0), 45.0);

        when(predictionService.getPrediction(eq("7A"), eq("Central Station"), any(LocalTime.class), anyBoolean()))
                .thenReturn(Optional.of(prediction));

        mockMvc.perform(get("/api/predictions")
                        .param("route", "7A")
                        .param("stop", "Central Station")
                        .param("time", "15:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stopName").value("Central Station"))
                .andExpect(jsonPath("$[0].time").value("15:00:00"))
                .andExpect(jsonPath("$[0].occupancyPercentage").value(45.0));

        verify(predictionService).getPrediction(eq("7A"), eq("Central Station"), any(LocalTime.class), eq(true));
    }

    @Test
    void testGetSinglePredictionReturnsNotFoundWhenNoData() throws Exception {
        when(predictionService.getPrediction(eq("7A"), eq("Central Station"), any(LocalTime.class), anyBoolean()))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/predictions")
                        .param("route", "7A")
                        .param("stop", "Central Station")
                        .param("time", "15:00"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetDailyPredictionsReturnsOk() throws Exception {
        var predictions = List.of(
                new OccupancyPredictionDTO("Central Station", LocalTime.of(8, 0), 45.0),
                new OccupancyPredictionDTO("Downtown", LocalTime.of(9, 0), 60.0)
        );

        when(predictionService.getTodayPredictions(eq("7A"), anyBoolean())).thenReturn(predictions);

        mockMvc.perform(get("/api/predictions")
                        .param("route", "7A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stopName").value("Central Station"))
                .andExpect(jsonPath("$[0].occupancyPercentage").value(45.0))
                .andExpect(jsonPath("$[1].stopName").value("Downtown"))
                .andExpect(jsonPath("$[1].occupancyPercentage").value(60.0));

        verify(predictionService).getTodayPredictions(eq("7A"), eq(true));
    }

    @Test
    void testGetPredictionsWithUseWeatherFalse() throws Exception {
        var predictions = List.of(
                new OccupancyPredictionDTO("Central Station", LocalTime.of(8, 0), 45.0)
        );

        when(predictionService.getTodayPredictions(eq("7A"), eq(false))).thenReturn(predictions);

        mockMvc.perform(get("/api/predictions")
                        .param("route", "7A")
                        .param("useWeather", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stopName").value("Central Station"))
                .andExpect(jsonPath("$[0].occupancyPercentage").value(45.0));

        verify(predictionService).getTodayPredictions(eq("7A"), eq(false));
    }
}
