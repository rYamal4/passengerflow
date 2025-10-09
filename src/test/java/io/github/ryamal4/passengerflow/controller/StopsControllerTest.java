package io.github.ryamal4.passengerflow.controller;

import io.github.ryamal4.passengerflow.dto.StopDTO;
import io.github.ryamal4.passengerflow.service.stop.IStopsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StopsController.class)
class StopsControllerTest {

    private static final String BASE_URL = "/api/stops";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IStopsService stopsService;

    private StopDTO stopDTO;

    @BeforeEach
    void setUp() {
        stopDTO = new StopDTO(1L, "Test Stop", 60.0, 24.0, 1L, "Test Route");
    }

    @Test
    void testGetNearbyStopsSuccess() throws Exception {
        var stops = List.of(stopDTO);
        when(stopsService.getNearbyStops(60.0, 24.0)).thenReturn(stops);

        mockMvc.perform(get(BASE_URL + "/nearby")
                        .param("lat", "60.0")
                        .param("lon", "24.0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Stop"))
                .andExpect(jsonPath("$[0].lat").value(60.0))
                .andExpect(jsonPath("$[0].lon").value(24.0))
                .andExpect(jsonPath("$[0].routeId").value(1))
                .andExpect(jsonPath("$[0].routeName").value("Test Route"));
    }

    @Test
    void testGetNearbyStopsEmptyList() throws Exception {
        when(stopsService.getNearbyStops(0.0, 0.0)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL + "/nearby")
                        .param("lat", "0.0")
                        .param("lon", "0.0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetNearbyStopsMissingLatParam() throws Exception {
        mockMvc.perform(get(BASE_URL + "/nearby")
                        .param("lon", "24.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetNearbyStopsMissingLonParam() throws Exception {
        mockMvc.perform(get(BASE_URL + "/nearby")
                        .param("lat", "60.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetNearbyStopsInvalidLatParam() throws Exception {
        mockMvc.perform(get(BASE_URL + "/nearby")
                        .param("lat", "invalid")
                        .param("lon", "24.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetNearbyStopsInvalidLonParam() throws Exception {
        mockMvc.perform(get(BASE_URL + "/nearby")
                        .param("lat", "60.0")
                        .param("lon", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllStopsSuccess() throws Exception {
        var stops = List.of(stopDTO);
        when(stopsService.getAllStops()).thenReturn(stops);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Stop"))
                .andExpect(jsonPath("$[0].lat").value(60.0))
                .andExpect(jsonPath("$[0].lon").value(24.0))
                .andExpect(jsonPath("$[0].routeId").value(1))
                .andExpect(jsonPath("$[0].routeName").value("Test Route"));
    }

    @Test
    void testGetAllStopsEmptyList() throws Exception {
        when(stopsService.getAllStops()).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}