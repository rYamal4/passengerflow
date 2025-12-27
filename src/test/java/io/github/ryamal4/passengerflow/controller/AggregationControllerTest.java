package io.github.ryamal4.passengerflow.controller;

import io.github.ryamal4.passengerflow.service.aggregation.IPassengerCountAggregationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;

import static org.mockito.Mockito.verify;
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
}
