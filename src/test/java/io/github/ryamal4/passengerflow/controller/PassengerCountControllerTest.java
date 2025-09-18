package io.github.ryamal4.passengerflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ryamal4.passengerflow.model.Bus;
import io.github.ryamal4.passengerflow.model.PassengerCount;
import io.github.ryamal4.passengerflow.model.Route;
import io.github.ryamal4.passengerflow.model.Stop;
import io.github.ryamal4.passengerflow.service.passenger.IPassengerCountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PassengerCountController.class)
class PassengerCountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IPassengerCountService passengerCountService;

    private PassengerCount passengerCount;

    @BeforeEach
    void setUp() {
        Route route = new Route();
        route.setId(1L);
        route.setName("Test Route");

        Bus bus = new Bus();
        bus.setId(1L);
        bus.setModel("Test Bus");
        bus.setRoute(route);

        Stop stop = new Stop();
        stop.setId(1L);
        stop.setName("Test Stop");
        stop.setLat(60.0);
        stop.setLon(24.0);
        stop.setRoute(route);

        this.passengerCount = new PassengerCount();
        this.passengerCount.setBus(bus);
        this.passengerCount.setStop(stop);
        this.passengerCount.setEntered(10);
        this.passengerCount.setExited(5);
        this.passengerCount.setTimestamp(LocalDateTime.of(2025, 9, 12, 12, 12, 12));
    }

    @Test
    void testCreateCountSuccess() throws Exception {
        PassengerCount savedCount = new PassengerCount();
        savedCount.setId(1L);
        savedCount.setBus(passengerCount.getBus());
        savedCount.setStop(passengerCount.getStop());
        savedCount.setEntered(passengerCount.getEntered());
        savedCount.setExited(passengerCount.getExited());
        savedCount.setTimestamp(passengerCount.getTimestamp());

        when(passengerCountService.createCount(passengerCount)).thenReturn(savedCount);

        mockMvc.perform(post("/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passengerCount)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.entered").value(10))
                .andExpect(jsonPath("$.exited").value(5))
                .andExpect(jsonPath("$.timestamp").value("2025-09-12T12:12:12"))
                .andExpect(jsonPath("$.bus.id").value(1))
                .andExpect(jsonPath("$.bus.model").value("Test Bus"))
                .andExpect(jsonPath("$.stop.id").value(1))
                .andExpect(jsonPath("$.stop.name").value("Test Stop"))
                .andExpect(jsonPath("$.stop.lat").value(60.0))
                .andExpect(jsonPath("$.stop.lon").value(24.0));
    }

    @Test
    void testCreateCountInvalidDataNegativeEntered() throws Exception {
        passengerCount.setEntered(-1);

        assertBadRequest();
    }

    @Test
    void testCreateCountInvalidDataNegativeExited() throws Exception {
        passengerCount.setExited(-1);

        assertBadRequest();
    }

    @ParameterizedTest
    @MethodSource("nullFieldSetters")
    void testCreateCountInvalidDataMissingField(Consumer<PassengerCount> fieldSetter) throws Exception {
        fieldSetter.accept(passengerCount);

        assertBadRequest();
    }

    @Test
    void testCreateCountEmptyBody() throws Exception {
        mockMvc.perform(post("/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private static Stream<Consumer<PassengerCount>> nullFieldSetters() {
        return Stream.of(
                pc -> pc.setBus(null),
                pc -> pc.setStop(null),
                pc -> pc.setEntered(null),
                pc -> pc.setExited(null),
                pc -> pc.setTimestamp(null)
        );
    }

    private void assertBadRequest() throws Exception {
        mockMvc.perform(post("/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passengerCount)))
                .andExpect(status().isBadRequest());
    }
}