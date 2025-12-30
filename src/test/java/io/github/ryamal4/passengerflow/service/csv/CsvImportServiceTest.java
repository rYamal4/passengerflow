package io.github.ryamal4.passengerflow.service.csv;

import io.github.ryamal4.passengerflow.model.BusModel;
import io.github.ryamal4.passengerflow.service.busmodel.IBusModelService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {

    private static final String VALID_CSV_CONTENT = """
            id,name,capacity
            1,Volvo 7900,50
            2,Mercedes Citaro,60
            3,MAN Lion's City,55
            """;
    @Mock
    private IBusModelService busModelService;
    @InjectMocks
    private CsvImportService csvImportService;

    @Test
    void testImportValidCsvSuccess() {
        var file = createCsvFile(VALID_CSV_CONTENT);

        var result = csvImportService.importBusModelsFromCsv(file);

        assertThat(result.getSuccessCount()).isEqualTo(3);
        assertThat(result.getFailedCount()).isZero();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.hasError()).isFalse();
        verify(busModelService, times(3)).create(any(BusModel.class));
    }

    @Test
    void testImportValidCsvParsesDataCorrectly() {
        var file = createCsvFile(VALID_CSV_CONTENT);
        var captor = ArgumentCaptor.forClass(BusModel.class);

        csvImportService.importBusModelsFromCsv(file);

        verify(busModelService, times(3)).create(captor.capture());
        var models = captor.getAllValues();

        assertThat(models.get(0).getId()).isEqualTo(1L);
        assertThat(models.get(0).getName()).isEqualTo("Volvo 7900");
        assertThat(models.get(0).getCapacity()).isEqualTo(50);

        assertThat(models.get(1).getId()).isEqualTo(2L);
        assertThat(models.get(1).getName()).isEqualTo("Mercedes Citaro");
        assertThat(models.get(1).getCapacity()).isEqualTo(60);

        assertThat(models.get(2).getId()).isEqualTo(3L);
        assertThat(models.get(2).getName()).isEqualTo("MAN Lion's City");
        assertThat(models.get(2).getCapacity()).isEqualTo(55);
    }

    @Test
    void testImportEmptyCsvReturnsZeroCounts() {
        var file = createCsvFile("id,name,capacity\n");

        var result = csvImportService.importBusModelsFromCsv(file);

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailedCount()).isZero();
        assertThat(result.getErrors()).isEmpty();
        verifyNoInteractions(busModelService);
    }

    @Test
    void testImportCsvWithInvalidIdReturnsError() {
        var content = """
                id,name,capacity
                invalid,Test Bus,50
                """;
        var file = createCsvFile(content);

        var result = csvImportService.importBusModelsFromCsv(file);

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).contains("некорректные данные");
        assertThat(result.hasError()).isTrue();
        verifyNoInteractions(busModelService);
    }

    @Test
    void testImportCsvWithInvalidCapacityReturnsError() {
        var content = """
                id,name,capacity
                1,Test Bus,invalid
                """;
        var file = createCsvFile(content);

        var result = csvImportService.importBusModelsFromCsv(file);

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).contains("некорректные данные");
        verifyNoInteractions(busModelService);
    }

    @Test
    void testImportCsvWithMixedValidAndInvalidRowsContinuesProcessing() {
        var content = """
                id,name,capacity
                1,Valid Bus,50
                invalid,Invalid Bus,60
                3,Another Valid,70
                """;
        var file = createCsvFile(content);

        var result = csvImportService.importBusModelsFromCsv(file);

        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).contains("некорректные данные");
        verify(busModelService, times(2)).create(any(BusModel.class));
    }

    @Test
    void testImportCsvWhenServiceThrowsExceptionCountsAsFailed() {
        var content = """
                id,name,capacity
                1,Existing Model,50
                2,New Model,60
                """;
        var file = createCsvFile(content);
        doThrow(new RuntimeException("Duplicate name")).when(busModelService).create(argThat(m -> m.getId() == 1L));

        var result = csvImportService.importBusModelsFromCsv(file);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isEqualTo(1);
        verify(busModelService, times(2)).create(any(BusModel.class));
    }

    @Test
    void testImportCsvWithIoExceptionReturnsError() throws IOException {
        var file = mock(MockMultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("File read error"));

        var result = csvImportService.importBusModelsFromCsv(file);

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailedCount()).isZero();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).contains("Не удалось прочитать файл");
        verifyNoInteractions(busModelService);
    }

    @Test
    void testImportCsvTrimsWhitespace() {
        var content = """
                id,name,capacity
                1,  Trimmed Name  ,50
                """;
        var file = createCsvFile(content);
        var captor = ArgumentCaptor.forClass(BusModel.class);

        csvImportService.importBusModelsFromCsv(file);

        verify(busModelService).create(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Trimmed Name");
    }

    @Test
    void testImportCsvIgnoresHeaderCase() {
        var content = """
                ID,NAME,CAPACITY
                1,Test Bus,50
                """;
        var file = createCsvFile(content);

        var result = csvImportService.importBusModelsFromCsv(file);

        assertThat(result.getSuccessCount()).isEqualTo(1);
        verify(busModelService).create(any(BusModel.class));
    }

    @Test
    void testImportCsvWithAllFailedSavesReturnsCorrectCounts() {
        var file = createCsvFile(VALID_CSV_CONTENT);
        doThrow(new RuntimeException("DB error")).when(busModelService).create(any(BusModel.class));

        var result = csvImportService.importBusModelsFromCsv(file);

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailedCount()).isEqualTo(3);
        verify(busModelService, times(3)).create(any(BusModel.class));
    }

    private MockMultipartFile createCsvFile(String content) {
        return new MockMultipartFile(
                "file",
                "bus_models.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}
