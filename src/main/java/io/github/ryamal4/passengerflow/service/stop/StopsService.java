package io.github.ryamal4.passengerflow.service.stop;

import io.github.ryamal4.passengerflow.dto.StopDTO;
import io.github.ryamal4.passengerflow.model.Stop;
import io.github.ryamal4.passengerflow.repository.IStopsRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class StopsService implements IStopsService {
    private static final int NEARBY_STOPS_COUNT = 5;

    private final IStopsRepository stopsRepository;

    public StopsService(IStopsRepository stopsRepository) {
        this.stopsRepository = stopsRepository;
    }

    @Override
    public List<StopDTO> getNearbyStops(double lat, double lon) {
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
        return stopsRepository.findNearbyStops(lat, lon, NEARBY_STOPS_COUNT).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public List<StopDTO> getAllStops() {
        return stopsRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    private StopDTO convertToDTO(Stop entity) {
        var dto = new StopDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setLat(entity.getLat());
        dto.setLon(entity.getLon());
        dto.setRouteId(entity.getRoute().getId());
        dto.setRouteName(entity.getRoute().getName());
        return dto;
    }
}
