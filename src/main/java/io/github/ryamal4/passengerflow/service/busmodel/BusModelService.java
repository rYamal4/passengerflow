package io.github.ryamal4.passengerflow.service.busmodel;

import io.github.ryamal4.passengerflow.dto.BusModelDTO;
import io.github.ryamal4.passengerflow.model.BusModel;
import io.github.ryamal4.passengerflow.repository.IBusModelRepository;
import io.github.ryamal4.passengerflow.service.IBusModelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BusModelService implements IBusModelService {
    private final IBusModelRepository repository;

    public BusModelService(IBusModelRepository repository) {
        this.repository = repository;
    }

    @Override
    public void create(BusModel model) {
        repository.save(model);
    }

    @Override
    public BusModel findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("BusModel not found with id: " + id));
    }

    @Override
    public BusModel update(BusModel model) {
        return repository.save(model);
    }

    public BusModelDTO convertToDTO(BusModel entity) {
        var dto = new BusModelDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCapacity(entity.getCapacity());
        dto.setFileName(entity.getFileName());
        return dto;
    }
}
