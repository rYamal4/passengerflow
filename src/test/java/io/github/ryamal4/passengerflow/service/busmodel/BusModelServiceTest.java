package io.github.ryamal4.passengerflow.service.busmodel;

import io.github.ryamal4.passengerflow.dto.BusModelDTO;
import io.github.ryamal4.passengerflow.model.BusModel;
import io.github.ryamal4.passengerflow.repository.IBusModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusModelServiceTest {

    @Mock
    private IBusModelRepository repository;

    @InjectMocks
    private BusModelService service;

    private BusModel testModel;

    @BeforeEach
    void setUp() {
        testModel = createBusModel(1L, "Model A", 50, null);
    }

    @Test
    void testCreateSuccess() {
        var model = createBusModel(null, "New Model", 60, null);
        when(repository.save(any(BusModel.class))).thenReturn(model);

        service.create(model);

        verify(repository).save(model);
    }

    @Test
    void testFindByIdSuccess() {
        when(repository.findById(1L)).thenReturn(Optional.of(testModel));

        var result = service.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Model A");
        assertThat(result.getCapacity()).isEqualTo(50);
        verify(repository).findById(1L);
    }

    @Test
    void testFindByIdNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BusModel not found with id: 999");

        verify(repository).findById(999L);
    }

    @Test
    void testUpdateSuccess() {
        var updatedModel = createBusModel(1L, "Model A", 50, "test-file.jpg");
        when(repository.save(any(BusModel.class))).thenReturn(updatedModel);

        var result = service.update(updatedModel);

        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isEqualTo("test-file.jpg");
        verify(repository).save(updatedModel);
    }

    @Test
    void testConvertToDTO() {
        var model = createBusModel(1L, "Model B", 70, "file.pdf");

        var dto = service.convertToDTO(model);

        assertDtoIsCorrect(dto, model);
    }

    @Test
    void testConvertToDTOWithNullFileName() {
        var model = createBusModel(2L, "Model C", 80, null);

        var dto = service.convertToDTO(model);

        assertDtoIsCorrect(dto, model);
        assertThat(dto.getFileName()).isNull();
    }

    private BusModel createBusModel(Long id, String name, Integer capacity, String fileName) {
        var model = new BusModel();
        model.setId(id);
        model.setName(name);
        model.setCapacity(capacity);
        model.setFileName(fileName);
        return model;
    }

    private void assertDtoIsCorrect(BusModelDTO dto, BusModel entity) {
        assertThat(dto.getId()).isEqualTo(entity.getId());
        assertThat(dto.getName()).isEqualTo(entity.getName());
        assertThat(dto.getCapacity()).isEqualTo(entity.getCapacity());
        assertThat(dto.getFileName()).isEqualTo(entity.getFileName());
    }
}
