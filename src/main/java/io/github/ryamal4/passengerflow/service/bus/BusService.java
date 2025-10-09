package io.github.ryamal4.passengerflow.service.bus;

import io.github.ryamal4.passengerflow.dto.BusDTO;
import io.github.ryamal4.passengerflow.model.Bus;
import io.github.ryamal4.passengerflow.repository.IBusRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class BusService implements IBusService {
    private final IBusRepository busRepository;

    public BusService(IBusRepository busRepository) {
        this.busRepository = busRepository;
    }

    @Override
    public List<BusDTO> getAllBuses() {
        return busRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    private BusDTO convertToDTO(Bus entity) {
        var dto = new BusDTO();
        dto.setId(entity.getId());
        dto.setBusModelId(entity.getBusModel().getId());
        dto.setBusModelName(entity.getBusModel().getName());
        dto.setBusModelCapacity(entity.getBusModel().getCapacity());
        dto.setRouteId(entity.getRoute().getId());
        dto.setRouteName(entity.getRoute().getName());
        return dto;
    }
}
