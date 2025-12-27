package io.github.ryamal4.passengerflow.controller;

import io.github.ryamal4.passengerflow.service.aggregation.IPassengerCountAggregationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DayOfWeek;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AggregationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AggregationControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IPassengerCountAggregationService aggregationService;

    @Test
    void testPerformAggregationCallsServiceAndReturnsOk() throws Exception {
        mockMvc.perform(post("/api/aggregation")
                        .param("dayOfWeek", "1"))
                .andExpect(status().isOk());

        verify(aggregationService).performAggregation(DayOfWeek.MONDAY);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7})
    void testPerformAggregationAllValidDaysOfWeek(int dayOfWeek) throws Exception {
        mockMvc.perform(post("/api/aggregation")
                        .param("dayOfWeek", String.valueOf(dayOfWeek)))
                .andExpect(status().isOk());

        verify(aggregationService).performAggregation(DayOfWeek.of(dayOfWeek));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "8", "-1", "100"})
    void testPerformAggregationInvalidDayOfWeek(String dayOfWeek) throws Exception {
        mockMvc.perform(post("/api/aggregation")
                        .param("dayOfWeek", dayOfWeek))
                .andExpect(status().isInternalServerError());

        verifyNoInteractions(aggregationService);
    }

    @Test
    void testPerformAggregationMissingDayOfWeekParam() throws Exception {
        mockMvc.perform(post("/api/aggregation"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(aggregationService);
    }

    @Test
    void testPerformAggregationInvalidDayOfWeekFormat() throws Exception {
        mockMvc.perform(post("/api/aggregation")
                        .param("dayOfWeek", "invalid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(aggregationService);
    }
}
