package io.github.ryamal4.passengerflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ryamal4.passengerflow.dto.PassengerCountDTO;
import io.github.ryamal4.passengerflow.service.passenger.IPassengerCountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PassengerCountController.class)
class PassengerCountControllerTest {

    private static final String BASE_URL = "/api/passengers";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IPassengerCountService passengerCountService;

    private PassengerCountDTO passengerCountDTO;

    @BeforeEach
    void setUp() {
        this.passengerCountDTO = new PassengerCountDTO();
        this.passengerCountDTO.setBusId(1L);
        this.passengerCountDTO.setStopId(1L);
        this.passengerCountDTO.setEntered(10);
        this.passengerCountDTO.setExited(5);
        this.passengerCountDTO.setTimestamp(LocalDateTime.of(2025, 9, 12, 12, 12, 12));
    }

    @Test
    void testCreateCountSuccess() throws Exception {
        var savedDTO = getPassengerCountDTO();

        when(passengerCountService.createCountFromDTO(any(PassengerCountDTO.class))).thenReturn(savedDTO);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passengerCountDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.busId").value(1))
                .andExpect(jsonPath("$.stopId").value(1))
                .andExpect(jsonPath("$.entered").value(10))
                .andExpect(jsonPath("$.exited").value(5))
                .andExpect(jsonPath("$.timestamp").value("2025-09-12T12:12:12"))
                .andExpect(jsonPath("$.busModel").value("Test Bus"))
                .andExpect(jsonPath("$.stopName").value("Test Stop"))
                .andExpect(jsonPath("$.routeName").value("Test Route"));
    }

    private PassengerCountDTO getPassengerCountDTO() {
        PassengerCountDTO savedDTO = new PassengerCountDTO();
        savedDTO.setId(1L);
        savedDTO.setBusId(passengerCountDTO.getBusId());
        savedDTO.setStopId(passengerCountDTO.getStopId());
        savedDTO.setEntered(passengerCountDTO.getEntered());
        savedDTO.setExited(passengerCountDTO.getExited());
        savedDTO.setTimestamp(passengerCountDTO.getTimestamp());
        savedDTO.setBusModel("Test Bus");
        savedDTO.setStopName("Test Stop");
        savedDTO.setRouteName("Test Route");
        return savedDTO;
    }

    @Test
    void testCreateCountInvalidDataNegativeEntered() throws Exception {
        passengerCountDTO.setEntered(-1);

        assertBadRequest();
    }

    @Test
    void testCreateCountInvalidDataNegativeExited() throws Exception {
        passengerCountDTO.setExited(-1);

        assertBadRequest();
    }

    @ParameterizedTest
    @MethodSource("nullFieldSetters")
    void testCreateCountInvalidDataMissingField(Consumer<PassengerCountDTO> fieldSetter) throws Exception {
        fieldSetter.accept(passengerCountDTO);

        assertBadRequest();
    }

    @Test
    void testCreateCountEmptyBody() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateCountIllegalStateException() throws Exception {
        when(passengerCountService.createCountFromDTO(any(PassengerCountDTO.class)))
                .thenThrow(new IllegalStateException("Invalid state"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passengerCountDTO)))
                .andExpect(status().isBadRequest());
    }

    private static Stream<Consumer<PassengerCountDTO>> nullFieldSetters() {
        return Stream.of(
                dto -> dto.setBusId(null),
                dto -> dto.setStopId(null),
                dto -> dto.setEntered(null),
                dto -> dto.setExited(null),
                dto -> dto.setTimestamp(null)
        );
    }

    private void assertBadRequest() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passengerCountDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllCountsSuccess() throws Exception {
        PassengerCountDTO dto = new PassengerCountDTO(1L, 1L, 1L, 10, 5,
                LocalDateTime.of(2025, 9, 12, 12, 12, 12),
                "Test Bus", "Test Stop", "Test Route");

        Page<PassengerCountDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);

        when(passengerCountService.getCountsByFilters(null, null, null, null, PageRequest.of(0, 20)))
                .thenReturn(page);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].busId").value(1))
                .andExpect(jsonPath("$.content[0].stopId").value(1))
                .andExpect(jsonPath("$.content[0].entered").value(10))
                .andExpect(jsonPath("$.content[0].exited").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.size").value(20));
    }

    @Test
    void testGetAllCountsWithFilters() throws Exception {
        PassengerCountDTO dto = new PassengerCountDTO(1L, 1L, 1L, 10, 5,
                LocalDateTime.of(2025, 9, 12, 12, 12, 12),
                "Test Bus", "Test Stop", "Test Route");

        Page<PassengerCountDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
        LocalDateTime startTime = LocalDateTime.of(2025, 9, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 9, 30, 23, 59);

        when(passengerCountService.getCountsByFilters(1L, 1L, startTime, endTime, PageRequest.of(0, 20)))
                .thenReturn(page);

        mockMvc.perform(get(BASE_URL)
                        .param("busId", "1")
                        .param("stopId", "1")
                        .param("startTime", "2025-09-01T00:00:00")
                        .param("endTime", "2025-09-30T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].busId").value(1))
                .andExpect(jsonPath("$.content[0].stopId").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void testGetAllCountsWithPagination() throws Exception {
        Page<PassengerCountDTO> page = new PageImpl<>(List.of(), PageRequest.of(2, 10), 25);

        when(passengerCountService.getCountsByFilters(null, null, null, null, PageRequest.of(2, 10)))
                .thenReturn(page);

        mockMvc.perform(get(BASE_URL)
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page.number").value(2))
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(25));
    }

    @Test
    void testGetAllCountsEmptyResult() throws Exception {
        Page<PassengerCountDTO> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(passengerCountService.getCountsByFilters(null, null, null, null, PageRequest.of(0, 20)))
                .thenReturn(page);

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void testGetCountByIdSuccess() throws Exception {
        PassengerCountDTO dto = new PassengerCountDTO(1L, 1L, 1L, 10, 5,
                LocalDateTime.of(2025, 9, 12, 12, 12, 12),
                "Test Bus", "Test Stop", "Test Route");

        when(passengerCountService.getCountById(1L)).thenReturn(Optional.of(dto));

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.busId").value(1))
                .andExpect(jsonPath("$.stopId").value(1))
                .andExpect(jsonPath("$.entered").value(10))
                .andExpect(jsonPath("$.exited").value(5))
                .andExpect(jsonPath("$.busModel").value("Test Bus"))
                .andExpect(jsonPath("$.stopName").value("Test Stop"))
                .andExpect(jsonPath("$.routeName").value("Test Route"));
    }

    @Test
    void testGetCountByIdNotFound() throws Exception {
        when(passengerCountService.getCountById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL + "/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateCountSuccess() throws Exception {
        PassengerCountDTO updateDTO = new PassengerCountDTO();
        updateDTO.setBusId(1L);
        updateDTO.setStopId(1L);
        updateDTO.setEntered(15);
        updateDTO.setExited(8);
        updateDTO.setTimestamp(LocalDateTime.of(2025, 9, 12, 12, 12, 12));

        PassengerCountDTO updatedDTO = new PassengerCountDTO(1L, 1L, 1L, 15, 8,
                LocalDateTime.of(2025, 9, 12, 12, 12, 12),
                "Test Bus", "Test Stop", "Test Route");

        when(passengerCountService.updateCount(eq(1L), any(PassengerCountDTO.class))).thenReturn(updatedDTO);

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.entered").value(15))
                .andExpect(jsonPath("$.exited").value(8));
    }

    @Test
    void testUpdateCountNotFound() throws Exception {
        PassengerCountDTO updateDTO = new PassengerCountDTO();
        updateDTO.setBusId(1L);
        updateDTO.setStopId(1L);
        updateDTO.setEntered(15);
        updateDTO.setExited(8);
        updateDTO.setTimestamp(LocalDateTime.of(2025, 9, 12, 12, 12, 12));

        when(passengerCountService.updateCount(eq(999L), any(PassengerCountDTO.class)))
                .thenThrow(new IllegalArgumentException("Not found"));

        mockMvc.perform(put(BASE_URL + "/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateCountInvalidData() throws Exception {
        PassengerCountDTO updateDTO = new PassengerCountDTO();
        updateDTO.setBusId(1L);
        updateDTO.setStopId(1L);
        updateDTO.setEntered(-1);
        updateDTO.setExited(8);
        updateDTO.setTimestamp(LocalDateTime.of(2025, 9, 12, 12, 12, 12));

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateCountIllegalStateException() throws Exception {
        PassengerCountDTO updateDTO = new PassengerCountDTO();
        updateDTO.setBusId(1L);
        updateDTO.setStopId(1L);
        updateDTO.setEntered(15);
        updateDTO.setExited(8);
        updateDTO.setTimestamp(LocalDateTime.of(2025, 9, 12, 12, 12, 12));

        when(passengerCountService.updateCount(eq(1L), any(PassengerCountDTO.class)))
                .thenThrow(new IllegalStateException("Invalid state"));

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteCountSuccess() throws Exception {
        doNothing().when(passengerCountService).deleteCount(1L);

        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNoContent());

        verify(passengerCountService, times(1)).deleteCount(1L);
    }

    @Test
    void testDeleteCountNotFound() throws Exception {
        doThrow(new IllegalArgumentException("Not found")).when(passengerCountService).deleteCount(999L);

        mockMvc.perform(delete(BASE_URL + "/999"))
                .andExpect(status().isNotFound());

        verify(passengerCountService, times(1)).deleteCount(999L);
    }
}