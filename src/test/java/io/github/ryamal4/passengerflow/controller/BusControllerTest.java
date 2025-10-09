package io.github.ryamal4.passengerflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ryamal4.passengerflow.dto.BusDTO;
import io.github.ryamal4.passengerflow.service.bus.IBusService;
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

@WebMvcTest(BusController.class)
class BusControllerTest {

    private static final String BASE_URL = "/api/buses";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IBusService busService;

    @Test
    void testGetAllBusesSuccess() throws Exception {
        var busDTO = new BusDTO(1L, 1L, "Test Bus Model", 50, 1L, "Test Route");
        var buses = List.of(busDTO);
        when(busService.getAllBuses()).thenReturn(buses);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].busModelId").value(1))
                .andExpect(jsonPath("$[0].busModelName").value("Test Bus Model"))
                .andExpect(jsonPath("$[0].busModelCapacity").value(50))
                .andExpect(jsonPath("$[0].routeId").value(1))
                .andExpect(jsonPath("$[0].routeName").value("Test Route"));
    }

    @Test
    void testGetAllBusesEmptyList() throws Exception {
        when(busService.getAllBuses()).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
