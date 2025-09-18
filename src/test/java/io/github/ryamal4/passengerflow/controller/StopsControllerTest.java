package io.github.ryamal4.passengerflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ryamal4.passengerflow.model.Route;
import io.github.ryamal4.passengerflow.model.Stop;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IStopsService stopsService;

    private Stop stop;

    @BeforeEach
    void setUp() {
        Route route = new Route();
        route.setId(1L);
        route.setName("Test Route");

        this.stop = new Stop();
        this.stop.setId(1L);
        this.stop.setName("Test Stop");
        this.stop.setLat(60.0);
        this.stop.setLon(24.0);
        this.stop.setRoute(route);
    }

    @Test
    void getNearbyStops_Success() throws Exception {
        List<Stop> stops = List.of(stop);

        when(stopsService.getNearbyStops(60.0, 24.0)).thenReturn(stops);

        mockMvc.perform(get("/stops/nearby")
                        .param("lat", "60.0")
                        .param("lon", "24.0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Stop"))
                .andExpect(jsonPath("$[0].lat").value(60.0))
                .andExpect(jsonPath("$[0].lon").value(24.0))
                .andExpect(jsonPath("$[0].route.id").value(1))
                .andExpect(jsonPath("$[0].route.name").value("Test Route"));
    }

    @Test
    void getNearbyStops_EmptyList() throws Exception {
        when(stopsService.getNearbyStops(0.0, 0.0)).thenReturn(List.of());

        mockMvc.perform(get("/stops/nearby")
                        .param("lat", "0.0")
                        .param("lon", "0.0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getNearbyStops_MissingLatParam() throws Exception {
        mockMvc.perform(get("/stops/nearby")
                        .param("lon", "24.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getNearbyStops_MissingLonParam() throws Exception {
        mockMvc.perform(get("/stops/nearby")
                        .param("lat", "60.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getNearbyStops_InvalidLatParam() throws Exception {
        mockMvc.perform(get("/stops/nearby")
                        .param("lat", "invalid")
                        .param("lon", "24.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getNearbyStops_InvalidLonParam() throws Exception {
        mockMvc.perform(get("/stops/nearby")
                        .param("lat", "60.0")
                        .param("lon", "invalid"))
                .andExpect(status().isBadRequest());
    }
}